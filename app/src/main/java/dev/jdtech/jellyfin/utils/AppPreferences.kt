package dev.jdtech.jellyfin.utils

import android.content.SharedPreferences
import android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
import androidx.core.content.edit
import javax.inject.Inject

class AppPreferences
@Inject
constructor(
    private val sharedPreferences: SharedPreferences
) {
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
}
