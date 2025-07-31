package com.example.pomodoros

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPreferences = application.getSharedPreferences("pomodoro_prefs", Application.MODE_PRIVATE)

    private val _alarmVolume = MutableLiveData<Int>()
    val alarmVolume: LiveData<Int> = _alarmVolume

    private val _backgroundVolume = MutableLiveData<Int>()
    val backgroundVolume: LiveData<Int> = _backgroundVolume

    init {
        _alarmVolume.value = sharedPreferences.getInt("alarm_volume", 100)
        _backgroundVolume.value = sharedPreferences.getInt("background_volume", 100)
    }

    fun setAlarmVolume(volume: Int) {
        _alarmVolume.value = volume
        sharedPreferences.edit().putInt("alarm_volume", volume).apply()
    }

    fun setBackgroundVolume(volume: Int) {
        _backgroundVolume.value = volume
        sharedPreferences.edit().putInt("background_volume", volume).apply()
    }
}
