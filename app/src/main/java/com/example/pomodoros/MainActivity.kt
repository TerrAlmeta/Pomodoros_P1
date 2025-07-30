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
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.Collections

class MainActivity : AppCompatActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private lateinit var adapter: TaskListAdapter
    private var currentTask: Task? = null
    private lateinit var timerReceiver: BroadcastReceiver

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

        val itemTouchHelper = ItemTouchHelper(
            ItemMoveCallback(
                { fromPosition, toPosition ->
                    val list = mainViewModel.allTasks.value?.toMutableList()
                    if (list != null) {
                        Collections.swap(list, fromPosition, toPosition)
                        updateTaskOrder(list)
                    }
                },
                { position ->
                    val task = mainViewModel.allTasks.value?.get(position)
                    if (task != null) {
                        mainViewModel.delete(task)
                    }
                })
        )
        itemTouchHelper.attachToRecyclerView(recyclerView)

        findViewById<Button>(R.id.start_button).setOnClickListener {
            currentTask?.let { startTimer(it) }
        }

        findViewById<Button>(R.id.pause_button).setOnClickListener {
            stopTimerService()
        }

        findViewById<Button>(R.id.restart_button).setOnClickListener {
            currentTask?.let {
                stopTimerService()
                startTimer(it)
            }
        }

        timerReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val millisUntilFinished = intent?.getLongExtra(TimerService.TIMER_VALUE, 0) ?: 0
                val minutes = (millisUntilFinished / 1000) / 60
                val seconds = (millisUntilFinished / 1000) % 60
                findViewById<TextView>(R.id.timer_text).text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
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

    private fun startTimer(task: Task) {
        val intent = Intent(this, TimerService::class.java)
        intent.putExtra("duration", task.pomodoroDuration * 60 * 1000L)
        intent.putExtra("alarmSound", task.pomodoroAlarmSound)
        intent.putExtra("backgroundSound", task.pomodoroBackgroundSound)
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
        findViewById<TextView>(R.id.task_title).text = currentTask?.name
        val duration = currentTask?.pomodoroDuration ?: 0
        findViewById<TextView>(R.id.timer_text).text = String.format(Locale.getDefault(), "%02d:00", duration)
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
}
