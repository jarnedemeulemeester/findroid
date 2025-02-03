package dev.jdtech.jellyfin.settings.domain

import android.content.SharedPreferences
import dev.jdtech.jellyfin.settings.domain.models.Preference
import javax.inject.Inject

class AppPreferences
@Inject
constructor(
    val sharedPreferences: SharedPreferences,
) {
    // Language
    val preferredAudioLanguage = Preference<String?>("pref_audio_language", null)
    val preferredSubtitleLanguage = Preference<String?>("pref_subtitle_language", null)

    // Player
    // Player - mpv
    val playerMpv = Preference("pref_player_mpv", true)
    val playerMpvHwdec = Preference("pref_player_mpv_hwdec", "mediacodec")
    val playerMpvVo = Preference("pref_player_mpv_vo", "gpu")
    val playerMpvAo = Preference("pref_player_mpv_ao", "audiotrack")

    // Player - gestures
    val playerGestures = Preference("pref_player_gestures", true)
    val playerGesturesVB = Preference("pref_player_gestures_vb", true)
    val playerGesturesZoom = Preference("pref_player_gestures_zoom", true)
    val playerGesturesSeek = Preference("pref_player_gestures_seek", true)
    val playerGesturesSeekTrickplay = Preference("pref_player_gestures_seek_trickplay", true)
    val playerGesturesChapterSkip = Preference("pref_player_gestures_chapter_skip", true)
    val playerGesturesBrightnessRemember = Preference("pref_player_brightness_remember", false)
    val playerGesturesStartMaximized = Preference("pref_player_start_maximized", false)

    // Player - seeking
    val playerSeekBackInc = Preference("pref_player_seek_back_inc", 5_000L)
    val playerSeekForwardInc = Preference("pref_player_seek_back_inc", 15_000L)
    val playerIntroSkipper = Preference("pref_player_intro_skipper", true)
    val playerChapterMarkers = Preference("pref_player_chapter_markers", true)

    // Player - trickplay
    val playerTrickplay = Preference("pref_player_trickplay", true)

    // Downloads
    val downloadOverMobileData = Preference("pref_downloads_mobile_data", false)
    val downloadWhenRoaming = Preference("pref_downloads_roaming", false)

    // Network
    val requestTimeout = Preference("pref_network_request_timeout", 30_000L)
    val connectTimeout = Preference("pref_network_connect_timeout", 6_000L)
    val socketTimeout = Preference("pref_network_socket_timeout", 10_000L)

    // Cache
    val imageCache = Preference("pref_image_cache", true)
    val imageCacheSize = Preference("pref_image_cache_size", 20)

    inline fun <reified T> getValue(preference: Preference<T>): T {
        return when (preference.defaultValue) {
            is Boolean -> sharedPreferences.getBoolean(preference.backendName, preference.defaultValue) as T
            is Int -> sharedPreferences.getInt(preference.backendName, preference.defaultValue) as T
            is Long -> sharedPreferences.getLong(preference.backendName, preference.defaultValue) as T
            is String? -> sharedPreferences.getString(preference.backendName, preference.defaultValue) as T
            else -> throw Exception()
        }
    }

    inline fun <reified T> setValue(preference: Preference<T>, value: T) {
        val editor = sharedPreferences.edit()
        when (preference.defaultValue) {
            is Boolean -> editor.putBoolean(preference.backendName, value as Boolean)
            is Int -> editor.putInt(preference.backendName, value as Int)
            is Long -> editor.putLong(preference.backendName, value as Long)
            is String? -> editor.putString(preference.backendName, value as String?)
            else -> throw Exception()
        }
        editor.apply()
    }
}
