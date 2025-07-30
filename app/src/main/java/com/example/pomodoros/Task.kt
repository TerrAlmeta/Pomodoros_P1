package com.example.pomodoros

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val pomodoroDuration: Int,
    val shortBreakDuration: Int,
    val longBreakDuration: Int,
    val cycles: Int,
    val pomodoroAlarmSound: String,
    val shortBreakAlarmSound: String,
    val longBreakAlarmSound: String,
    val pomodoroBackgroundSound: String,
    val shortBreakBackgroundSound: String,
    val longBreakBackgroundSound: String,
    val color: String,
    val order: Int
)
