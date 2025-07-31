package com.example.pomodoros

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import java.util.Locale
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import android.os.Handler
import android.graphics.Color
import android.os.Looper
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.Collections

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

        val swipeHandler = object : SwipeToEditCallback(this, this) {}
        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(recyclerView)


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
                if (millisUntilFinished > 0) {
                    val minutes = (millisUntilFinished / 1000) / 60
                    val seconds = (millisUntilFinished / 1000) % 60
                    findViewById<TextView>(R.id.timer_text).text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
                } else {
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun startTimer(duration: Long, alarmSound: String?, backgroundSound: String?) {
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

    private fun updateTaskOrder(tasks: List<Task>) {
        for (i in tasks.indices) {
            val task = tasks[i].copy(order = i)
            mainViewModel.update(task)
        }
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
            batteryOptimizationIntent.data = Uri.parse("package:$packageName")
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
}
