package com.example.pomodoros

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val _language = MutableLiveData<String>()
    val language: LiveData<String> = _language

    private val _alarmVolume = MutableLiveData<Int>()
    val alarmVolume: LiveData<Int> = _alarmVolume

    private val _backgroundVolume = MutableLiveData<Int>()
    val backgroundVolume: LiveData<Int> = _backgroundVolume

    private val _distractionFreeMode = MutableLiveData<Boolean>()
    val distractionFreeMode: LiveData<Boolean> = _distractionFreeMode

    fun setLanguage(lang: String) {
        _language.value = lang
    }

    fun setAlarmVolume(volume: Int) {
        _alarmVolume.value = volume
    }

    fun setBackgroundVolume(volume: Int) {
        _backgroundVolume.value = volume
    }

    fun setDistractionFreeMode(enabled: Boolean) {
        _distractionFreeMode.value = enabled
    }
}
