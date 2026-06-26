package dev.jdtech.jellyfin.player.cast.models

enum class CastConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

data class CastPlaybackState(
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L
)

open class Device(
    open val id: String,
    open val name: String,
    open val description: String? = null,
    open val enabled: Boolean = true
)
