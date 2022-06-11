package dev.jdtech.jellyfin.utils

object Constants {
    // player
    const val GESTURE_EXCLUSION_AREA_TOP = 48
    const val FULL_SWIPE_RANGE_SCREEN_RATIO = 0.66f
    const val ZOOM_SCALE_BASE = 1f
    const val ZOOM_SCALE_THRESHOLD = 0.01f

    // pref
    const val PREF_PLAYER_GESTURES = "pref_player_gestures"
    const val PREF_PLAYER_GESTURES_VB = "pref_player_gestures_vb"
    const val PREF_PLAYER_GESTURES_ZOOM = "pref_player_gestures_zoom"
    const val PREF_PLAYER_BRIGHTNESS_REMEMBER = "pref_player_brightness_remember"
    const val PREF_PLAYER_BRIGHTNESS = "pref_player_brightness"
    const val PREF_IMAGE_CACHE = "pref_image_cache"
    const val PREF_IMAGE_CACHE_SIZE = "pref_image_cache_size"

    // caching
    const val DEFAULT_CACHE_SIZE = 20
}