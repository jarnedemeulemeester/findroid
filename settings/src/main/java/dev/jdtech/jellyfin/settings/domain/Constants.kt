package dev.jdtech.jellyfin.settings.domain

object Constants {
    // Player - Media Segments
    const val PLAYER_MEDIA_SEGMENTS_DEFAULT_SKIP_BUTTON_DURATION = 5L
    const val PLAYER_MEDIA_SEGMENTS_DEFAULT_NEXT_EPISODE_THRESHOLD = 5_000L
    object PlayerMediaSegmentsAutoSkip {
        const val ALWAYS = "always"
        const val PIP = "pip"
    }

    // Network
    const val NETWORK_DEFAULT_REQUEST_TIMEOUT = 30_000L
    const val NETWORK_DEFAULT_CONNECT_TIMEOUT = 6_000L
    const val NETWORK_DEFAULT_SOCKET_TIMEOUT = 10_000L
}
