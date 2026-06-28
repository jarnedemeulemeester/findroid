package dev.jdtech.jellyfin.player.cast.models

enum class CastConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

enum class CastPlaybackStatus {
    IDLE,
    BUFFERING,
    READY,
    PAUSED,
    PLAYING,
    ENDED,
    ERROR
}

data class CastPlayerState(
    val status: CastPlaybackStatus = CastPlaybackStatus.IDLE,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val volume: Float = 1f,
    val isMuted: Boolean = false,
    val hasNextItem: Boolean = false,
    val hasPreviousItem: Boolean = false
)

open class Device(
    open val id: String,
    open val name: String,
    open val description: String? = null,
    open val enabled: Boolean = true
)
