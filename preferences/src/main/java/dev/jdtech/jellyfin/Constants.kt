package dev.jdtech.jellyfin

object Constants {

    // player
    const val GESTURE_EXCLUSION_AREA_VERTICAL = 48
    const val GESTURE_EXCLUSION_AREA_HORIZONTAL = 24
    const val FULL_SWIPE_RANGE_SCREEN_RATIO = 0.66f
    const val ZOOM_SCALE_BASE = 1f
    const val ZOOM_SCALE_THRESHOLD = 0.01f

    // pref
    const val PREF_CURRENT_SERVER = "pref_current_server"
    const val PREF_OFFLINE_MODE = "pref_offline_mode"
    const val PREF_PLAYER_GESTURES = "pref_player_gestures"
    const val PREF_PLAYER_GESTURES_VB = "pref_player_gestures_vb"
    const val PREF_PLAYER_GESTURES_ZOOM = "pref_player_gestures_zoom"
    const val PREF_PLAYER_GESTURES_SEEK = "pref_player_gestures_seek"
    const val PREF_PLAYER_BRIGHTNESS_REMEMBER = "pref_player_brightness_remember"
    const val PREF_PLAYER_BRIGHTNESS = "pref_player_brightness"
    const val PREF_PLAYER_SEEK_BACK_INC = "pref_player_seek_back_inc"
    const val PREF_PLAYER_SEEK_FORWARD_INC = "pref_player_seek_forward_inc"
    const val PREF_PLAYER_MPV = "pref_player_mpv"
    const val PREF_PLAYER_MPV_HWDEC = "pref_player_mpv_hwdec"
    const val PREF_PLAYER_MPV_HWDEC_CODECS = "pref_player_mpv_hwdec_codecs"
    const val PREF_PLAYER_MPV_VO = "pref_player_mpv_vo"
    const val PREF_PLAYER_MPV_AO = "pref_player_mpv_ao"
    const val PREF_PLAYER_MPV_GPU_API = "pref_player_mpv_gpu_api"
    const val PREF_PLAYER_INTRO_SKIPPER = "pref_player_intro_skipper"
    const val PREF_PLAYER_TRICK_PLAY = "pref_player_trick_play"
    const val PREF_PLAYER_PIP_GESTURE = "pref_player_picture_in_picture_gesture"
    const val PREF_AUDIO_LANGUAGE = "pref_audio_language"
    const val PREF_SUBTITLE_LANGUAGE = "pref_subtitle_language"
    const val PREF_IMAGE_CACHE = "pref_image_cache"
    const val PREF_IMAGE_CACHE_SIZE = "pref_image_cache_size"
    const val PREF_THEME = "theme"
    const val PREF_DYNAMIC_COLORS = "dynamic_colors"
    const val PREF_AMOLED_THEME = "pref_amoled_theme"
    const val PREF_NETWORK_REQUEST_TIMEOUT = "pref_network_request_timeout"
    const val PREF_NETWORK_CONNECT_TIMEOUT = "pref_network_connect_timeout"
    const val PREF_NETWORK_SOCKET_TIMEOUT = "pref_network_socket_timeout"
    const val PREF_DOWNLOADS_MOBILE_DATA = "pref_downloads_mobile_data"
    const val PREF_DOWNLOADS_ROAMING = "pref_downloads_roaming"
    const val PREF_SORT_BY = "pref_sort_by"
    const val PREF_SORT_ORDER = "pref_sort_order"
    const val PREF_DISPLAY_EXTRA_INFO = "pref_display_extra_info"

    // caching
    const val DEFAULT_CACHE_SIZE = 20

    // favorites
    const val FAVORITE_TYPE_MOVIES = 0
    const val FAVORITE_TYPE_SHOWS = 1
    const val FAVORITE_TYPE_EPISODES = 2

    // network
    const val NETWORK_DEFAULT_REQUEST_TIMEOUT = 30_000L
    const val NETWORK_DEFAULT_CONNECT_TIMEOUT = 6_000L
    const val NETWORK_DEFAULT_SOCKET_TIMEOUT = 10_000L

    // sorting
    // This values must correspond to a SortString from [SortBy]
    const val DEFAULT_SORT_BY = "SortName"
    const val DEFAULT_SORT_ORDER = "Ascending"
}
