package dev.jdtech.jellyfin.player.cast

import dev.jdtech.jellyfin.player.cast.models.CastPlayerState
import dev.jdtech.jellyfin.player.core.domain.models.PlayerItem
import dev.jdtech.jellyfin.player.core.domain.models.Track
import kotlinx.coroutines.flow.StateFlow
import org.jellyfin.sdk.model.UUID

interface CastPlayerController {
    val currentItem: StateFlow<PlayerItem?>
    val playerState: StateFlow<CastPlayerState>
    val subtitleTracks: StateFlow<List<Track>>
    val audioTracks: StateFlow<List<Track>>

    fun playItem(itemId: UUID, itemKind: String, startFromBeginning: Boolean)
    fun play()
    fun pause()
    fun seekTo(position: Long)
    fun seekToNext()
    fun seekToPrevious()
    fun setVolume(volume: Float)
    fun setAudioTrack(track: Track?, itemId: UUID?)
    fun setSubtitleTrack(track: Track?)
    fun stop()
}
