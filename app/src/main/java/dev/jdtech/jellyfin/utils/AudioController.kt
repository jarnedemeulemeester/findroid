package dev.jdtech.jellyfin.utils

import android.content.Context
import android.media.AudioManager
import android.media.AudioManager.ADJUST_LOWER
import android.media.AudioManager.ADJUST_RAISE
import android.media.AudioManager.ADJUST_SAME
import android.media.AudioManager.FLAG_SHOW_UI
import android.media.AudioManager.STREAM_MUSIC

internal class AudioController internal constructor(context: Context) {

    private val manager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun volumeUp() = manager.adjustStreamVolume(STREAM_MUSIC, ADJUST_RAISE, FLAG_SHOW_UI)
    fun volumeDown() = manager.adjustStreamVolume(STREAM_MUSIC, ADJUST_LOWER, FLAG_SHOW_UI)
    fun showVolumeSlider() = manager.adjustStreamVolume(STREAM_MUSIC, ADJUST_SAME, FLAG_SHOW_UI)
}