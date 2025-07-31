package com.example.pomodoros

import android.app.NotificationManager
import android.content.Context
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.google.android.material.switchmaterial.SwitchMaterial
import java.util.Locale

class SettingsActivity : AppCompatActivity() {

    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val languageSpinner = findViewById<Spinner>(R.id.language_spinner)
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

        val permissionsStatusTextView = findViewById<TextView>(R.id.permissions_status_text_view)
        updatePermissionsStatus(permissionsStatusTextView)

        findViewById<Button>(R.id.manage_permissions_button).setOnClickListener {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = android.net.Uri.fromParts("package", packageName, null)
            intent.data = uri
            startActivity(intent)
        }

        val distractionFreeSwitch = findViewById<SwitchMaterial>(R.id.distraction_free_switch)
        distractionFreeSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                requestDoNotDisturb()
            } else {
                turnOffDoNotDisturb()
            }
        }

        val alarmVolumeSeekBar = findViewById<SeekBar>(R.id.alarm_volume_seek_bar)
        settingsViewModel.alarmVolume.observe(this) { volume ->
            alarmVolumeSeekBar.progress = volume
        }
        alarmVolumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                settingsViewModel.setAlarmVolume(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        val backgroundVolumeSeekBar = findViewById<SeekBar>(R.id.background_volume_seek_bar)
        settingsViewModel.backgroundVolume.observe(this) { volume ->
            backgroundVolumeSeekBar.progress = volume
        }
        backgroundVolumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                settingsViewModel.setBackgroundVolume(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
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

    private fun setLocale(languageCode: String) {
        val locale = Locale.forLanguageTag(languageCode)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        baseContext.resources.updateConfiguration(config, baseContext.resources.displayMetrics)

        val sharedPref = getSharedPreferences("pomodoro_prefs", MODE_PRIVATE)
        sharedPref.edit {
            putString("language", languageCode)
        }
        recreate()
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
