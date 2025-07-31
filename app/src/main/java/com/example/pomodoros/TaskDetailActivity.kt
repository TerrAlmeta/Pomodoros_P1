package com.example.pomodoros

import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity

class TaskDetailActivity : AppCompatActivity() {

    private val taskDetailViewModel: TaskDetailViewModel by viewModels()
    private var taskId: Int = -1
    private var selectedColor: String = ""
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_detail)

        taskId = intent.getIntExtra("TASK_ID", -1)

        val taskNameEditText = findViewById<EditText>(R.id.task_name_edit_text)
        val pomodoroDurationEditText = findViewById<EditText>(R.id.pomodoro_duration_edit_text)
        val shortBreakDurationEditText = findViewById<EditText>(R.id.short_break_duration_edit_text)
        val longBreakDurationEditText = findViewById<EditText>(R.id.long_break_duration_edit_text)
        val cyclesEditText = findViewById<EditText>(R.id.cycles_edit_text)

        val alarmSounds = listOf("Vibration", "alarm1", "alarm4", "alarm5", "alarm7", "alarm10", "alarm11", "alarm3", "alarm12", "alarm6", "alarm8", "alarm9")
        val backgroundSounds = listOf("None", "background1", "background6", "background10", "background2", "background4", "background8", "background9", "background3", "background5", "background7")

        val pomodoroAlarmSpinner = findViewById<Spinner>(R.id.pomodoro_alarm_spinner)
        val shortBreakAlarmSpinner = findViewById<Spinner>(R.id.short_break_alarm_spinner)
        val longBreakAlarmSpinner = findViewById<Spinner>(R.id.long_break_alarm_spinner)
        val pomodoroBackgroundSpinner = findViewById<Spinner>(R.id.pomodoro_background_spinner)
        val shortBreakBackgroundSpinner = findViewById<Spinner>(R.id.short_break_background_spinner)
        val longBreakBackgroundSpinner = findViewById<Spinner>(R.id.long_break_background_spinner)

        val alarmAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, alarmSounds)
        alarmAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        pomodoroAlarmSpinner.adapter = alarmAdapter
        shortBreakAlarmSpinner.adapter = alarmAdapter
        longBreakAlarmSpinner.adapter = alarmAdapter

        val backgroundAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, backgroundSounds)
        backgroundAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        pomodoroBackgroundSpinner.adapter = backgroundAdapter
        shortBreakBackgroundSpinner.adapter = backgroundAdapter
        longBreakBackgroundSpinner.adapter = backgroundAdapter

        if (taskId != -1) {
            taskDetailViewModel.getTaskById(taskId).observe(this) { task ->
                task?.let {
                    taskNameEditText.setText(it.name)
                    pomodoroDurationEditText.setText(it.pomodoroDuration.toString())
                    shortBreakDurationEditText.setText(it.shortBreakDuration.toString())
                    longBreakDurationEditText.setText(it.longBreakDuration.toString())
                    cyclesEditText.setText(it.cycles.toString())
                    pomodoroAlarmSpinner.setSelection(alarmSounds.indexOf(it.pomodoroAlarmSound))
                    shortBreakAlarmSpinner.setSelection(alarmSounds.indexOf(it.shortBreakAlarmSound))
                    longBreakAlarmSpinner.setSelection(alarmSounds.indexOf(it.longBreakAlarmSound))
                    pomodoroBackgroundSpinner.setSelection(backgroundSounds.indexOf(it.pomodoroBackgroundSound))
                    shortBreakBackgroundSpinner.setSelection(backgroundSounds.indexOf(it.shortBreakBackgroundSound))
                    longBreakBackgroundSpinner.setSelection(backgroundSounds.indexOf(it.longBreakBackgroundSound))
                    selectedColor = it.color
                }
            }
        }

        findViewById<View>(R.id.color_1).setOnClickListener { selectedColor = "#FF7F50" }
        findViewById<View>(R.id.color_2).setOnClickListener { selectedColor = "#6495ED" }
        findViewById<View>(R.id.color_3).setOnClickListener { selectedColor = "#9FE2BF" }
        findViewById<View>(R.id.color_4).setOnClickListener { selectedColor = "#DE3163" }
        findViewById<View>(R.id.color_5).setOnClickListener { selectedColor = "#FFBF00" }

        findViewById<Button>(R.id.save_button).setOnClickListener {
            val taskName = taskNameEditText.text.toString()
            val pomodoroDuration = pomodoroDurationEditText.text.toString()
            val shortBreakDuration = shortBreakDurationEditText.text.toString()
            val longBreakDuration = longBreakDurationEditText.text.toString()
            val cycles = cyclesEditText.text.toString()

            if (taskName.isEmpty() || pomodoroDuration.isEmpty() || shortBreakDuration.isEmpty() || longBreakDuration.isEmpty() || cycles.isEmpty()) {
                // Show an error message
                return@setOnClickListener
            }

            val pomodoroAlarm = pomodoroAlarmSpinner.selectedItem.toString()
            val shortBreakAlarm = shortBreakAlarmSpinner.selectedItem.toString()
            val longBreakAlarm = longBreakAlarmSpinner.selectedItem.toString()
            val pomodoroBackground = pomodoroBackgroundSpinner.selectedItem.toString()
            val shortBreakBackground = shortBreakBackgroundSpinner.selectedItem.toString()
            val longBreakBackground = longBreakBackgroundSpinner.selectedItem.toString()

            if (taskId != -1) {
                val task = Task(
                    id = taskId,
                    name = taskName,
                    pomodoroDuration = pomodoroDuration.toInt(),
                    shortBreakDuration = shortBreakDuration.toInt(),
                    longBreakDuration = longBreakDuration.toInt(),
                    cycles = cycles.toInt(),
                    pomodoroAlarmSound = pomodoroAlarm,
                    shortBreakAlarmSound = shortBreakAlarm,
                    longBreakAlarmSound = longBreakAlarm,
                    pomodoroBackgroundSound = pomodoroBackground,
                    shortBreakBackgroundSound = shortBreakBackground,
                    longBreakBackgroundSound = longBreakBackground,
                    color = selectedColor,
                    order = 0
                )
                taskDetailViewModel.update(task)
            } else {
                val task = Task(
                    name = taskName,
                    pomodoroDuration = pomodoroDuration.toInt(),
                    shortBreakDuration = shortBreakDuration.toInt(),
                    longBreakDuration = longBreakDuration.toInt(),
                    cycles = cycles.toInt(),
                    pomodoroAlarmSound = pomodoroAlarm,
                    shortBreakAlarmSound = shortBreakAlarm,
                    longBreakAlarmSound = longBreakAlarm,
                    pomodoroBackgroundSound = pomodoroBackground,
                    shortBreakBackgroundSound = shortBreakBackground,
                    longBreakBackgroundSound = longBreakBackground,
                    color = selectedColor,
                    order = 0
                )
                taskDetailViewModel.insert(task)
            }
            finish()
        }

        findViewById<Button>(R.id.cancel_button).setOnClickListener {
            finish()
        }

        val soundSelectionListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val soundName = parent?.getItemAtPosition(position).toString()
                if (soundName != "None" && soundName != "Vibration") {
                    playSound(soundName)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }

        pomodoroAlarmSpinner.onItemSelectedListener = soundSelectionListener
        shortBreakAlarmSpinner.onItemSelectedListener = soundSelectionListener
        longBreakAlarmSpinner.onItemSelectedListener = soundSelectionListener
        pomodoroBackgroundSpinner.onItemSelectedListener = soundSelectionListener
        shortBreakBackgroundSpinner.onItemSelectedListener = soundSelectionListener
        longBreakBackgroundSpinner.onItemSelectedListener = soundSelectionListener
    }

    private fun playSound(soundName: String) {
        mediaPlayer?.release()
        val resId = getSoundResId(soundName)
        if (resId != 0) {
            mediaPlayer = MediaPlayer.create(this, resId)
            mediaPlayer?.start()
        }
    }

    private fun getSoundResId(soundName: String): Int {
        return when (soundName) {
            "alarm1" -> R.raw.alarm1
            "alarm3" -> R.raw.alarm3
            "alarm4" -> R.raw.alarm4
            "alarm5" -> R.raw.alarm5
            "alarm6" -> R.raw.alarm6
            "alarm7" -> R.raw.alarm7
            "alarm8" -> R.raw.alarm8
            "alarm9" -> R.raw.alarm9
            "alarm10" -> R.raw.alarm10
            "alarm11" -> R.raw.alarm11
            "alarm12" -> R.raw.alarm12
            "background1" -> R.raw.background1
            "background2" -> R.raw.background2
            "background3" -> R.raw.background3
            "background4" -> R.raw.background4
            "background5" -> R.raw.background5
            "background6" -> R.raw.background6
            "background7" -> R.raw.background7
            "background8" -> R.raw.background8
            "background9" -> R.raw.background9
            "background10" -> R.raw.background10
            else -> 0
        }
    }

    override fun onStop() {
        super.onStop()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
