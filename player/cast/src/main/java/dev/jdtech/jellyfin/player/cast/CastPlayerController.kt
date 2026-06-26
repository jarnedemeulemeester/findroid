package dev.jdtech.jellyfin.player.cast

import dev.jdtech.jellyfin.player.cast.models.CastPlaybackState
import dev.jdtech.jellyfin.player.core.domain.models.PlayerItem
import dev.jdtech.jellyfin.player.core.domain.models.Track
import kotlinx.coroutines.flow.StateFlow
import org.jellyfin.sdk.model.UUID
import org.jellyfin.sdk.model.api.PlaybackInfoResponse

interface CastPlayerController {
    val currentItem: StateFlow<PlayerItem?>
    val restoringItemId: StateFlow<String?>
    val playbackState: StateFlow<CastPlaybackState>
    val subtitleTracks: StateFlow<List<Track>>
    val audioTracks: StateFlow<List<Track>>
    val volume: StateFlow<Float>

    val playbackInfoResponse: StateFlow<PlaybackInfoResponse?>

    fun loadItem(item: PlayerItem, startPosition: Long)
    fun restoreItem(item: PlayerItem)
    fun queueNextItem(item: PlayerItem)
    fun queuePreviousItem(item: PlayerItem)
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
