package dev.jdtech.jellyfin

import android.content.SharedPreferences
import android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
import androidx.core.content.edit
import androidx.media3.common.C.DEFAULT_SEEK_BACK_INCREMENT_MS
import androidx.media3.common.C.DEFAULT_SEEK_FORWARD_INCREMENT_MS
import javax.inject.Inject

class AppPreferences
@Inject
constructor(
    private val sharedPreferences: SharedPreferences
) {
    // Server
    var currentServer: String?
        get() = sharedPreferences.getString(Constants.PREF_CURRENT_SERVER, null)
        set(value) {
            sharedPreferences.edit {
                putString(Constants.PREF_CURRENT_SERVER, value)
            }
        }

    // Appearance
    val theme get() = sharedPreferences.getString(Constants.PREF_THEME, null)
    val dynamicColors get() = sharedPreferences.getBoolean(Constants.PREF_DYNAMIC_COLORS, true)

    // Player
    val displayExtendedTitle get() = sharedPreferences.getBoolean(Constants.PREF_DISPLAY_EXTENDED_TITLE, false)

    val playerGestures get() = sharedPreferences.getBoolean(Constants.PREF_PLAYER_GESTURES, true)
    val playerGesturesVB get() = sharedPreferences.getBoolean(Constants.PREF_PLAYER_GESTURES_VB, true)
    val playerGesturesZoom get() = sharedPreferences.getBoolean(Constants.PREF_PLAYER_GESTURES_ZOOM, true)

    val playerBrightnessRemember get() =
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
    val playerSeekBackIncrement get() = sharedPreferences.getString(
        Constants.PREF_PLAYER_SEEK_BACK_INC,
        DEFAULT_SEEK_BACK_INCREMENT_MS.toString()
    )!!.toLongOrNull() ?: DEFAULT_SEEK_BACK_INCREMENT_MS
    val playerSeekForwardIncrement get() = sharedPreferences.getString(
        Constants.PREF_PLAYER_SEEK_FORWARD_INC,
        DEFAULT_SEEK_FORWARD_INCREMENT_MS.toString()
    )!!.toLongOrNull() ?: DEFAULT_SEEK_FORWARD_INCREMENT_MS
    val playerMpv get() = sharedPreferences.getBoolean(Constants.PREF_PLAYER_MPV, false)
    val playerMpvHwdec get() = sharedPreferences.getString(Constants.PREF_PLAYER_MPV_HWDEC, "mediacodec-copy")!!
    val playerMpvHwdecCodecs: Set<String> get() = sharedPreferences.getStringSet(
        Constants.PREF_PLAYER_MPV_HWDEC_CODECS,
        setOf("h264", "hevc", "mpeg4", "mpeg2video", "vp8", "vp9")
    )!!
    val playerMpvVo get() = sharedPreferences.getString(Constants.PREF_PLAYER_MPV_VO, "gpu")!!
    val playerMpvAo get() = sharedPreferences.getString(Constants.PREF_PLAYER_MPV_AO, "audiotrack")!!
    val playerMpvGpuApi get() = sharedPreferences.getString(Constants.PREF_PLAYER_MPV_GPU_API, "opengl")!!

    // Language
    val preferredAudioLanguage get() = sharedPreferences.getString(Constants.PREF_AUDIO_LANGUAGE, "")!!
    val preferredSubtitleLanguage get() = sharedPreferences.getString(Constants.PREF_SUBTITLE_LANGUAGE, "")!!

    // Network
    val requestTimeout get() = sharedPreferences.getString(
        Constants.PREF_NETWORK_REQUEST_TIMEOUT,
        Constants.NETWORK_DEFAULT_REQUEST_TIMEOUT.toString()
    )!!.toLongOrNull() ?: Constants.NETWORK_DEFAULT_REQUEST_TIMEOUT

    val connectTimeout get() = sharedPreferences.getString(
        Constants.PREF_NETWORK_CONNECT_TIMEOUT,
        Constants.NETWORK_DEFAULT_CONNECT_TIMEOUT.toString()
    )!!.toLongOrNull() ?: Constants.NETWORK_DEFAULT_CONNECT_TIMEOUT

    val socketTimeout get() = sharedPreferences.getString(
        Constants.PREF_NETWORK_SOCKET_TIMEOUT,
        Constants.NETWORK_DEFAULT_SOCKET_TIMEOUT.toString()
    )!!.toLongOrNull() ?: Constants.NETWORK_DEFAULT_SOCKET_TIMEOUT
}
