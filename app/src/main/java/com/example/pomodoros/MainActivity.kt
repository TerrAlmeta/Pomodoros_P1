package com.example.pomodoros

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import android.app.NotificationManager
import androidx.core.net.toUri
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.switchmaterial.SwitchMaterial
import java.util.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SeekBar

class MainActivity : AppCompatActivity(), SwipeToEditCallback.SwipeToEditCallbackListener {

    private val mainViewModel: MainViewModel by viewModels()
    private lateinit var adapter: TaskListAdapter
    private var currentTask: Task? = null
    private lateinit var timerReceiver: BroadcastReceiver
    private var isTimerRunning = false
    private var currentCycle = 1
    private var currentTimerType = "pomodoro" // "pomodoro", "short_break", "long_break"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        requestPermissions()

        val recyclerView = findViewById<RecyclerView>(R.id.task_recycler_view)
        adapter = TaskListAdapter()
        recyclerView.adapter = adapter

        mainViewModel.allTasks.observe(this) { tasks ->
            tasks?.let {
                adapter.submitList(it)
                if (it.isNotEmpty()) {
                    currentTask = it[0]
                    updateCurrentTaskUI()
                }
            }
        }

        val fab = findViewById<FloatingActionButton>(R.id.add_task_fab)
        fab.setOnClickListener {
            val intent = Intent(this@MainActivity, TaskDetailActivity::class.java)
            startActivity(intent)
        }

        val navView = findViewById<com.google.android.material.navigation.NavigationView>(R.id.nav_view)
        val headerView = navView.getHeaderView(0)
        headerView.findViewById<View>(R.id.close_nav_drawer_button).setOnClickListener {
            findViewById<androidx.drawerlayout.widget.DrawerLayout>(R.id.drawer_layout).close()
        }

        val swipeHandler = object : SwipeToEditCallback(this, this) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // Do nothing here. The actions will be handled by click listeners on the buttons.
            }
        }
        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(recyclerView)

        val languageSpinner = navView.findViewById<Spinner>(R.id.language_spinner)
        val languages = listOf("English", "Español")
        val languageAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages)
        languageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        languageSpinner.adapter = languageAdapter
        languageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val language = if (position == 0) "en" else "es"
                val currentLanguage = Locale.getDefault().language
                if (language != currentLanguage) {
                    setLocale(language)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val permissionsStatusTextView = navView.findViewById<TextView>(R.id.permissions_status_text_view)
        updatePermissionsStatus(permissionsStatusTextView)

        navView.findViewById<Button>(R.id.manage_permissions_button).setOnClickListener {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = "package:$packageName".toUri()
            intent.data = uri
            startActivity(intent)
        }

        val distractionFreeSwitch = navView.findViewById<SwitchMaterial>(R.id.distraction_free_switch)
        distractionFreeSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                requestDoNotDisturb()
            } else {
                turnOffDoNotDisturb()
            }
        }

        val alarmVolumeSeekBar = navView.findViewById<SeekBar>(R.id.alarm_volume_seek_bar)
        val sharedPreferences = getSharedPreferences("pomodoro_prefs", MODE_PRIVATE)
        alarmVolumeSeekBar.progress = sharedPreferences.getInt("alarm_volume", 100)
        alarmVolumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                sharedPreferences.edit().putInt("alarm_volume", progress).apply()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        val backgroundVolumeSeekBar = navView.findViewById<SeekBar>(R.id.background_volume_seek_bar)
        backgroundVolumeSeekBar.progress = sharedPreferences.getInt("background_volume", 100)
        backgroundVolumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                sharedPreferences.edit().putInt("background_volume", progress).apply()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })


        findViewById<Button>(R.id.start_button).setOnClickListener {
            currentTask?.let {
                currentCycle = 1
                currentTimerType = "pomodoro"
                startTimer(it.pomodoroDuration * 60 * 1000L, it.pomodoroAlarmSound, it.pomodoroBackgroundSound)
                isTimerRunning = true
            }
        }

        findViewById<Button>(R.id.pause_button).setOnClickListener {
            stopTimerService()
            isTimerRunning = false
        }

        findViewById<Button>(R.id.restart_button).setOnClickListener {
            currentTask?.let {
                stopTimerService()
                currentCycle = 1
                currentTimerType = "pomodoro"
                startTimer(it.pomodoroDuration * 60 * 1000L, it.pomodoroAlarmSound, it.pomodoroBackgroundSound)
                isTimerRunning = true
            }
        }

        timerReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val millisUntilFinished = intent?.getLongExtra(TimerService.TIMER_VALUE, 0) ?: 0
                Log.d("MainActivity", "onReceive called with millisUntilFinished: $millisUntilFinished")
                val minutes = (millisUntilFinished / 1000) / 60
                val seconds = (millisUntilFinished / 1000) % 60
                val timerTextView = findViewById<TextView>(R.id.timer_text)
                timerTextView.text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
                Log.d("MainActivity", "timer_text updated to: ${timerTextView.text}")
                if (millisUntilFinished == 0L) {
                    isTimerRunning = false
                    handleTimerFinish()
                }
            }
        }
        val intentFilter = IntentFilter(TimerService.TIMER_UPDATE)
        ContextCompat.registerReceiver(this, timerReceiver, intentFilter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(timerReceiver)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    private fun startTimer(duration: Long, alarmSound: String?, backgroundSound: String?) {
        Log.d("MainActivity", "startTimer called with duration: $duration")
        val intent = Intent(this, TimerService::class.java)
        intent.putExtra("duration", duration)
        intent.putExtra("alarmSound", alarmSound)
        intent.putExtra("backgroundSound", backgroundSound)
        startService(intent)
    }

    private fun stopTimerService() {
        val intent = Intent(this, TimerService::class.java)
        stopService(intent)
    }

    private fun updateCurrentTaskUI() {
        val taskTitleTextView = findViewById<TextView>(R.id.task_title)
        val timerTextView = findViewById<TextView>(R.id.timer_text)

        taskTitleTextView.text = currentTask?.name
        if (!isTimerRunning) {
            val duration = currentTask?.pomodoroDuration ?: 0
            timerTextView.text = String.format(Locale.getDefault(), "%02d:00", duration)
        }

        currentTask?.color?.let {
            if (it.isNotEmpty()) {
                val color = Color.parseColor(it)
                taskTitleTextView.setTextColor(color)
                timerTextView.setTextColor(color)
            }
        }
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val batteryOptimizationIntent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            batteryOptimizationIntent.data = "package:$packageName".toUri()
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}.launch(batteryOptimizationIntent)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val notificationPolicyIntent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}.launch(notificationPolicyIntent)
        }
    }

    override fun attachBaseContext(newBase: Context) {
        val sharedPref = newBase.getSharedPreferences("pomodoro_prefs", MODE_PRIVATE)
        val language = sharedPref.getString("language", "en") ?: "en"
        val locale = Locale.forLanguageTag(language)
        val config = newBase.resources.configuration
        config.setLocale(locale)
        applyOverrideConfiguration(config)
        super.attachBaseContext(newBase)
    }

    private fun handleTimerFinish() {
        currentTask?.let { task ->
            when (currentTimerType) {
                "pomodoro" -> {
                    if (currentCycle < task.cycles) {
                        currentTimerType = "short_break"
                        findViewById<TextView>(R.id.task_title).text = getString(R.string.short_break)
                        Handler(Looper.getMainLooper()).postDelayed({
                            startTimer(task.shortBreakDuration * 60 * 1000L, task.shortBreakAlarmSound, task.shortBreakBackgroundSound)
                        }, 1000)
                    } else {
                        currentTimerType = "long_break"
                        findViewById<TextView>(R.id.task_title).text = getString(R.string.long_break)
                        Handler(Looper.getMainLooper()).postDelayed({
                            startTimer(task.longBreakDuration * 60 * 1000L, task.longBreakAlarmSound, task.longBreakBackgroundSound)
                        }, 1000)
                    }
                }
                "short_break" -> {
                    currentCycle++
                    currentTimerType = "pomodoro"
                    findViewById<TextView>(R.id.task_title).text = task.name
                    Handler(Looper.getMainLooper()).postDelayed({
                        startTimer(task.pomodoroDuration * 60 * 1000L, task.pomodoroAlarmSound, task.pomodoroBackgroundSound)
                    }, 1000)
                }
                "long_break" -> {
                    // Task finished
                    currentCycle = 1
                    currentTimerType = "pomodoro"
                    updateCurrentTaskUI()
                }
            }
        }
    }

    override fun onEditClicked(position: Int) {
        val task = adapter.currentList[position]
        val intent = Intent(this, TaskDetailActivity::class.java)
        intent.putExtra("TASK_ID", task.id)
        startActivity(intent)
    }

    override fun onDeleteClicked(position: Int) {
        val task = adapter.currentList[position]
        mainViewModel.delete(task)
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int) {
        val list = adapter.currentList.toMutableList()
        Collections.swap(list, fromPosition, toPosition)
        adapter.notifyItemMoved(fromPosition, toPosition)
        updateTaskOrder(list)
    }

    override fun onSwiped(position: Int) {
        val task = adapter.currentList[position]
        mainViewModel.delete(task)
    }

    private fun updateTaskOrder(tasks: List<Task>) {
        for (i in tasks.indices) {
            val task = tasks[i].copy(order = i)
            mainViewModel.update(task)
        }
    }

    private fun setLocale(languageCode: String) {
        val locale = Locale.forLanguageTag(languageCode)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        baseContext.resources.updateConfiguration(config, baseContext.resources.displayMetrics)

        val sharedPref = getSharedPreferences("pomodoro_prefs", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("language", languageCode)
            apply()
        }
        recreate()
    }

    private fun updatePermissionsStatus(textView: TextView) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notificationManager.isNotificationPolicyAccessGranted) {
            textView.text = getString(R.string.all_permissions_granted)
        } else {
            textView.text = getString(R.string.some_permissions_are_missing)
        }
    }

    private fun requestDoNotDisturb() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !notificationManager.isNotificationPolicyAccessGranted) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            startActivity(intent)
        } else {
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
        }
    }

    private fun turnOffDoNotDisturb() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        }
    }
}
