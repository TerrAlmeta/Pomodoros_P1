package com.example.pomodoros

import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView

class SoundSpinnerAdapter(
    context: Context,
    private val soundNames: Array<String>,
    private val soundFiles: Map<String, String>,
    private val getSoundResId: (String) -> Int,
    private val onPlayPauseClicked: (String) -> Unit
) : ArrayAdapter<String>(context, 0, soundNames) {

    private var mediaPlayer: MediaPlayer? = null
    private var currentlyPlaying: String? = null

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createDropdownView(position, convertView, parent)
    }

    private fun createView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(android.R.layout.simple_spinner_item, parent, false)
        val textView = view.findViewById<TextView>(android.R.id.text1)
        textView.text = soundNames[position]
        textView.setTextColor(context.resources.getColor(R.color.white, null))
        return view
    }

    private fun createDropdownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.spinner_item_with_play, parent, false)

        val soundNameTextView = view.findViewById<TextView>(R.id.sound_name_text_view)
        val playPauseButton = view.findViewById<ImageView>(R.id.play_pause_button)

        val soundName = soundNames[position]
        soundNameTextView.text = soundName

        if (soundName == currentlyPlaying) {
            playPauseButton.setImageResource(R.drawable.ic_pause)
        } else {
            playPauseButton.setImageResource(R.drawable.ic_play_arrow)
        }

        playPauseButton.setOnClickListener {
            if (soundName == currentlyPlaying) {
                // Pause the sound
                mediaPlayer?.pause()
                currentlyPlaying = null
                notifyDataSetChanged()
            } else {
                // Stop any previously playing sound
                mediaPlayer?.release()
                mediaPlayer = null
                currentlyPlaying = soundName

                // Play the new sound
                val fileName = soundFiles[soundName]
                if (fileName == "Vibration") {
                    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
                        vibratorManager.defaultVibrator
                    } else {
                        @Suppress("DEPRECATION")
                        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(3000, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        //deprecated in API 26
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(3000)
                    }
                } else if (fileName != null && fileName != "None") {
                    val resId = getSoundResId(fileName)
                    if (resId != 0) {
                        mediaPlayer = MediaPlayer.create(context, resId)
                        mediaPlayer?.setOnCompletionListener {
                            currentlyPlaying = null
                            notifyDataSetChanged()
                        }
                        mediaPlayer?.start()
                    }
                }
                notifyDataSetChanged()
            }
        }
        return view
    }
}
