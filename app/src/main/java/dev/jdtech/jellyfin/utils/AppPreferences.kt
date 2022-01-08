package dev.jdtech.jellyfin.utils

import android.content.Context
import android.content.SharedPreferences
import android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AppPreferences @Inject constructor(@ApplicationContext context: Context) {
    private val sharedPreferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)

    var playerBrightness: Float
        get() = sharedPreferences.getFloat(Constants.PREF_PLAYER_BRIGHTNESS, BRIGHTNESS_OVERRIDE_NONE)
        set(value) {
           sharedPreferences.edit {
               putFloat(Constants.PREF_PLAYER_BRIGHTNESS,value)
           }
        }

}
