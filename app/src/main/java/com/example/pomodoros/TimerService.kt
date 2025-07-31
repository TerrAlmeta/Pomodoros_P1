package com.example.pomodoros

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import android.os.Vibrator
import android.net.Uri
import android.os.VibrationEffect
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class TimerService : Service() {

    private val notificationChannelId = "TimerServiceChannel"
    private val notificationId = 1
    private var countDownTimer: CountDownTimer? = null
    private var mediaPlayer: MediaPlayer? = null
    private var alarmPlayer: MediaPlayer? = null

    companion object {
        const val TIMER_UPDATE = "TIMER_UPDATE"
        const val TIMER_VALUE = "TIMER_VALUE"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("TimerService", "onStartCommand called")
        createNotificationChannel()

        val duration = intent?.getLongExtra("duration", 0) ?: 0
        val alarmSound = intent?.getStringExtra("alarmSound")
        val backgroundSound = intent?.getStringExtra("backgroundSound")

        Log.d("TimerService", "duration: $duration, alarmSound: $alarmSound, backgroundSound: $backgroundSound")

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, notificationChannelId)
            .setContentTitle("Pomodoros")
            .setContentText("Timer is running...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(notificationId, notification)

        playBackgroundSound(backgroundSound)

        countDownTimer = object : CountDownTimer(duration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                Log.d("TimerService", "onTick: $millisUntilFinished")
                val intent = Intent(TIMER_UPDATE)
                intent.putExtra(TIMER_VALUE, millisUntilFinished)
                sendBroadcast(intent)
            }

            override fun onFinish() {
                Log.d("TimerService", "onFinish")
                val intent = Intent(TIMER_UPDATE)
                intent.putExtra(TIMER_VALUE, 0L)
                sendBroadcast(intent)
                stopBackgroundSound()
                playAlarm(alarmSound)
            }
        }.start()

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        stopBackgroundSound()
        alarmPlayer?.release()
        alarmPlayer = null
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                notificationChannelId,
                "Timer Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(serviceChannel)
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

    private fun playBackgroundSound(soundName: String?) {
        Log.d("TimerService", "playBackgroundSound called with soundName: $soundName")
        if (soundName != null && soundName != "None") {
            mediaPlayer?.release()
            val resId = getSoundResId(soundName)
            if (resId != 0) {
                val sharedPreferences = getSharedPreferences("pomodoro_prefs", MODE_PRIVATE)
                val volume = sharedPreferences.getInt("background_volume", 100) / 100f
                mediaPlayer = MediaPlayer.create(this, resId)
                mediaPlayer?.isLooping = true
                mediaPlayer?.setVolume(volume, volume)
                mediaPlayer?.start()
                Log.d("TimerService", "MediaPlayer started")
            }
        }
    }

    private fun stopBackgroundSound() {
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.stop()
        }
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun playAlarm(alarmSound: String?) {
        val sharedPreferences = getSharedPreferences("pomodoro_prefs", MODE_PRIVATE)
        val alarmVolume = sharedPreferences.getInt("alarm_volume", 100)

        if (alarmSound == "Vibration" || alarmVolume == 0) {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(VIBRATOR_SERVICE) as Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(3000, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                //deprecated in API 26
                @Suppress("DEPRECATION")
                vibrator.vibrate(3000)
            }
        } else if (alarmSound != null) {
            val resId = getSoundResId(alarmSound)
            if (resId != 0) {
                val volume = alarmVolume / 100f
                alarmPlayer = MediaPlayer.create(this, resId)
                alarmPlayer?.setVolume(volume, volume)
                alarmPlayer?.setOnCompletionListener {
                    it.release()
                    alarmPlayer = null
                }
                alarmPlayer?.start()
            }
        }
    }
}
