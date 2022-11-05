package dev.jdtech.jellyfin.utils

import android.content.SharedPreferences
import android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
import androidx.core.content.edit
import com.google.android.exoplayer2.C.DEFAULT_SEEK_BACK_INCREMENT_MS
import com.google.android.exoplayer2.C.DEFAULT_SEEK_FORWARD_INCREMENT_MS
import javax.inject.Inject

class AppPreferences
@Inject
constructor(
    private val sharedPreferences: SharedPreferences
) {
    // Appearance
    val theme = sharedPreferences.getString(Constants.PREF_THEME, null)
    val dynamicColors = sharedPreferences.getBoolean(Constants.PREF_DYNAMIC_COLORS, true)

    // Player
    val playerGestures = sharedPreferences.getBoolean(Constants.PREF_PLAYER_GESTURES, true)
    val playerGesturesVB = sharedPreferences.getBoolean(Constants.PREF_PLAYER_GESTURES_VB, true)
    val playerGesturesZoom = sharedPreferences.getBoolean(Constants.PREF_PLAYER_GESTURES_ZOOM, true)

    val playerBrightnessRemember =
        sharedPreferences.getBoolean(Constants.PREF_PLAYER_BRIGHTNESS_REMEMBER, false)

    var playerBrightness: Float
        get() = sharedPreferences.getFloat(
            Constants.PREF_PLAYER_BRIGHTNESS,
            BRIGHTNESS_OVERRIDE_NONE
        )
        set(value) {
            sharedPreferences.edit {
                putFloat(Constants.PREF_PLAYER_BRIGHTNESS, value)
            }
        }
    val playerSeekBackIncrement = sharedPreferences.getString(
        Constants.PREF_PLAYER_SEEK_BACK_INC,
        DEFAULT_SEEK_BACK_INCREMENT_MS.toString()
    )!!.toLongOrNull() ?: DEFAULT_SEEK_BACK_INCREMENT_MS
    val playerSeekForwardIncrement = sharedPreferences.getString(
        Constants.PREF_PLAYER_SEEK_FORWARD_INC,
        DEFAULT_SEEK_FORWARD_INCREMENT_MS.toString()
    )!!.toLongOrNull() ?: DEFAULT_SEEK_FORWARD_INCREMENT_MS
    val mpvDisableHwDec = sharedPreferences.getBoolean("mpv_disable_hwdec", false)

    // Network
    val requestTimeout = sharedPreferences.getString(
        Constants.PREF_NETWORK_REQUEST_TIMEOUT,
        Constants.NETWORK_DEFAULT_REQUEST_TIMEOUT.toString()
    )!!.toLongOrNull() ?: Constants.NETWORK_DEFAULT_REQUEST_TIMEOUT

    val connectTimeout = sharedPreferences.getString(
        Constants.PREF_NETWORK_CONNECT_TIMEOUT,
        Constants.NETWORK_DEFAULT_CONNECT_TIMEOUT.toString()
    )!!.toLongOrNull() ?: Constants.NETWORK_DEFAULT_CONNECT_TIMEOUT

    val socketTimeout = sharedPreferences.getString(
        Constants.PREF_NETWORK_SOCKET_TIMEOUT,
        Constants.NETWORK_DEFAULT_SOCKET_TIMEOUT.toString()
    )!!.toLongOrNull() ?: Constants.NETWORK_DEFAULT_SOCKET_TIMEOUT
}
