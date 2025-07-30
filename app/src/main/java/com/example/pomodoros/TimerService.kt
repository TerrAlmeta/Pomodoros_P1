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
import androidx.core.app.NotificationCompat

class TimerService : Service() {

    private val NOTIFICATION_CHANNEL_ID = "TimerServiceChannel"
    private val NOTIFICATION_ID = 1
    private var countDownTimer: CountDownTimer? = null
    private var mediaPlayer: MediaPlayer? = null

    companion object {
        const val TIMER_UPDATE = "TIMER_UPDATE"
        const val TIMER_VALUE = "TIMER_VALUE"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()

        val duration = intent?.getLongExtra("duration", 0) ?: 0
        val alarmSound = intent?.getStringExtra("alarmSound")
        val backgroundSound = intent?.getStringExtra("backgroundSound")

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Pomodoros")
            .setContentText("Timer is running...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        playBackgroundSound(backgroundSound)

        countDownTimer = object : CountDownTimer(duration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val intent = Intent(TIMER_UPDATE)
                intent.putExtra(TIMER_VALUE, millisUntilFinished)
                sendBroadcast(intent)
            }

            override fun onFinish() {
                stopBackgroundSound()
                playAlarm(alarmSound)
                stopSelf()
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
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Timer Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun playBackgroundSound(soundName: String?) {
        if (soundName != null && soundName != "None") {
            mediaPlayer?.release()
            val resId = resources.getIdentifier(soundName, "raw", packageName)
            mediaPlayer = MediaPlayer.create(this, resId)
            mediaPlayer?.isLooping = true
            mediaPlayer?.start()
        }
    }

    private fun stopBackgroundSound() {
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun playAlarm(alarmSound: String?) {
        if (alarmSound == "Vibration") {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(3000)
        } else if (alarmSound != null) {
            val resId = resources.getIdentifier(alarmSound, "raw", packageName)
            val alarmPlayer = MediaPlayer.create(this, resId)
            alarmPlayer.start()
            alarmPlayer.setOnCompletionListener { it.release() }
        }
    }
}
