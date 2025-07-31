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
import androidx.core.content.ContextCompat

class TaskDetailActivity : AppCompatActivity() {

    private val taskDetailViewModel: TaskDetailViewModel by viewModels()
    private var taskId: Int = -1
    private var selectedColor: String = ""
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var colorViews: List<View>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_detail)

        taskId = intent.getIntExtra("TASK_ID", -1)

        val taskNameEditText = findViewById<EditText>(R.id.task_name_edit_text)
        val pomodoroDurationEditText = findViewById<EditText>(R.id.pomodoro_duration_edit_text)
        val shortBreakDurationEditText = findViewById<EditText>(R.id.short_break_duration_edit_text)
        val longBreakDurationEditText = findViewById<EditText>(R.id.long_break_duration_edit_text)
        val cyclesEditText = findViewById<EditText>(R.id.cycles_edit_text)

        val alarmSoundNames = resources.getStringArray(R.array.alarm_sound_names)
        val backgroundSoundNames = resources.getStringArray(R.array.background_sound_names)

        val alarmSounds = mapOf(
            "Vibration" to "Vibration", "Alarm 1" to "alarm1", "Alarm 2" to "alarm4", "Alarm 3" to "alarm5",
            "Alarm 4" to "alarm7", "Alarm 5" to "alarm10", "Alarm 6" to "alarm11", "Applause" to "alarm3",
            "Bell" to "alarm12", "Splash" to "alarm6", "I'm fine" to "alarm8", "Knock Knock" to "alarm9"
        )

        val backgroundSounds = mapOf(
            "None" to "None", "Keyboard" to "background1", "Piano" to "background6", "Meditation" to "background10",
            "Fire" to "background2", "Crystal" to "background4", "Rain" to "background8", "Wind" to "background9",
            "Bubbles" to "background3", "Coffee Shop" to "background5", "Steps" to "background7"
        )

        val pomodoroAlarmSpinner = findViewById<Spinner>(R.id.pomodoro_alarm_spinner)
        val shortBreakAlarmSpinner = findViewById<Spinner>(R.id.short_break_alarm_spinner)
        val longBreakAlarmSpinner = findViewById<Spinner>(R.id.long_break_alarm_spinner)
        val pomodoroBackgroundSpinner = findViewById<Spinner>(R.id.pomodoro_background_spinner)
        val shortBreakBackgroundSpinner = findViewById<Spinner>(R.id.short_break_background_spinner)
        val longBreakBackgroundSpinner = findViewById<Spinner>(R.id.long_break_background_spinner)

        val alarmAdapter = SoundSpinnerAdapter(this, alarmSoundNames.toTypedArray(), alarmSounds, ::getSoundResId) { soundName ->
            // This is where we would handle the play/pause click if we needed to do something in the activity
        }
        pomodoroAlarmSpinner.adapter = alarmAdapter
        shortBreakAlarmSpinner.adapter = alarmAdapter
        longBreakAlarmSpinner.adapter = alarmAdapter

        val backgroundAdapter = SoundSpinnerAdapter(this, backgroundSoundNames.toTypedArray(), backgroundSounds, ::getSoundResId) { soundName ->
            // This is where we would handle the play/pause click if we needed to do something in the activity
        }
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

                    val pomodoroAlarmName = alarmSounds.entries.find { it.value == task.pomodoroAlarmSound }?.key
                    pomodoroAlarmSpinner.setSelection(alarmSoundNames.indexOf(pomodoroAlarmName))

                    val shortBreakAlarmName = alarmSounds.entries.find { it.value == task.shortBreakAlarmSound }?.key
                    shortBreakAlarmSpinner.setSelection(alarmSoundNames.indexOf(shortBreakAlarmName))

                    val longBreakAlarmName = alarmSounds.entries.find { it.value == task.longBreakAlarmSound }?.key
                    longBreakAlarmSpinner.setSelection(alarmSoundNames.indexOf(longBreakAlarmName))

                    val pomodoroBackgroundName = backgroundSounds.entries.find { it.value == task.pomodoroBackgroundSound }?.key
                    pomodoroBackgroundSpinner.setSelection(backgroundSoundNames.indexOf(pomodoroBackgroundName))

                    val shortBreakBackgroundName = backgroundSounds.entries.find { it.value == task.shortBreakBackgroundSound }?.key
                    shortBreakBackgroundSpinner.setSelection(backgroundSoundNames.indexOf(shortBreakBackgroundName))

                    val longBreakBackgroundName = backgroundSounds.entries.find { it.value == task.longBreakBackgroundSound }?.key
                    longBreakBackgroundSpinner.setSelection(backgroundSoundNames.indexOf(longBreakBackgroundName))

                    selectedColor = it.color
                }
            }
        }

        colorViews = listOf(
            findViewById(R.id.color_1),
            findViewById(R.id.color_2),
            findViewById(R.id.color_3),
            findViewById(R.id.color_4),
            findViewById(R.id.color_5)
        )

        colorViews[0].setOnClickListener { updateColorSelection(it, "#FF7F50") }
        colorViews[1].setOnClickListener { updateColorSelection(it, "#6495ED") }
        colorViews[2].setOnClickListener { updateColorSelection(it, "#9FE2BF") }
        colorViews[3].setOnClickListener { updateColorSelection(it, "#DE3163") }
        colorViews[4].setOnClickListener { updateColorSelection(it, "#FFBF00") }

        val colors = listOf("#FF7F50", "#6495ED", "#9FE2BF", "#DE3163", "#FFBF00")
        if (taskId != -1) {
            taskDetailViewModel.getTaskById(taskId).observe(this) { task ->
                task?.let {
                    // ... (rest of the task loading logic)
                    selectedColor = it.color
                    val colorIndex = colors.indexOf(selectedColor)
                    if (colorIndex != -1) {
                        updateColorSelection(colorViews[colorIndex], selectedColor)
                    }
                }
            }
        } else {
            // Set a default color
            updateColorSelection(colorViews[0], colors[0])
        }

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

            val pomodoroAlarm = alarmSounds[pomodoroAlarmSpinner.selectedItem.toString()]
            val shortBreakAlarm = alarmSounds[shortBreakAlarmSpinner.selectedItem.toString()]
            val longBreakAlarm = alarmSounds[longBreakAlarmSpinner.selectedItem.toString()]
            val pomodoroBackground = backgroundSounds[pomodoroBackgroundSpinner.selectedItem.toString()]
            val shortBreakBackground = backgroundSounds[shortBreakBackgroundSpinner.selectedItem.toString()]
            val longBreakBackground = backgroundSounds[longBreakBackgroundSpinner.selectedItem.toString()]

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

    }

    private fun playSound(soundName: String) {
        mediaPlayer?.release()
        val resId = getSoundResId(soundName)
        if (resId != 0) {
            val sharedPreferences = getSharedPreferences("pomodoro_prefs", Context.MODE_PRIVATE)
            val volume = sharedPreferences.getInt("alarm_volume", 100) / 100f
            mediaPlayer = MediaPlayer.create(this, resId)
            mediaPlayer?.setVolume(volume, volume)
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

    private fun updateColorSelection(selectedView: View, color: String) {
        selectedColor = color
        for (view in colorViews) {
            view.foreground = null
        }
        selectedView.foreground = ContextCompat.getDrawable(this, R.drawable.color_selection_border)
    }
}
