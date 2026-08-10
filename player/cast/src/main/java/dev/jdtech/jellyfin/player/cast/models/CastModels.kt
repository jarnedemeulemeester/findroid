package dev.jdtech.jellyfin.player.cast.models

import dev.jdtech.jellyfin.player.core.domain.models.PlayerItem
import dev.jdtech.jellyfin.player.core.domain.models.Track
import org.jellyfin.sdk.model.api.PlaybackInfoResponse

enum class CastConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

enum class CastPlaybackStatus {
    IDLE,
    BUFFERING,
    PAUSED,
    PLAYING,
    ENDED,
    ERROR
}

open class Device(
    open val id: String,
    open val name: String,
    open val enabled: Boolean = true,
    open val supportsH265: Boolean = false
)

data class CastPlayerState(
    val status: CastPlaybackStatus = CastPlaybackStatus.IDLE,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val volume: Float = 1f,
    val isMuted: Boolean = false,
    val hasNextItem: Boolean = false,
    val hasPreviousItem: Boolean = false
)

data class CastMediaItem(
    val item: PlayerItem,
    val playbackInfo: PlaybackInfoResponse? = null,
    val subtitleTracks: List<Track> = emptyList(),
    val audioTracks: List<Track> = emptyList()
)
