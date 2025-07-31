package com.example.pomodoros

data class Sound(val name: String, val resourceId: Int) {
    override fun toString(): String {
        return name
    }
}
