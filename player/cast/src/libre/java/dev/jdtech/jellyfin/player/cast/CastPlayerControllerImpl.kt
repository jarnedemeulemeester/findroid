package dev.jdtech.jellyfin.player.cast

import dev.jdtech.jellyfin.player.cast.models.CastMediaItem
import dev.jdtech.jellyfin.player.cast.models.CastPlayerState
import dev.jdtech.jellyfin.player.core.domain.models.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.jellyfin.sdk.model.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CastPlayerControllerImpl @Inject constructor() : CastPlayerController {
    private val _currentItem = MutableStateFlow<CastMediaItem?>(null)
    override val currentItem: StateFlow<CastMediaItem?> = _currentItem.asStateFlow()

    private val _playerState = MutableStateFlow(CastPlayerState())
    override val playerState: StateFlow<CastPlayerState> = _playerState.asStateFlow()

    override fun playItem(itemId: UUID, itemKind: String, startFromBeginning: Boolean) {}
    override fun play() {}
    override fun pause() {}
    override fun seekTo(position: Long) {}
    override fun seekToNext() {}
    override fun seekToPrevious() {}
    override fun setVolume(volume: Float) {}
    override fun setAudioTrack(track: Track?, itemId: UUID?) {}
    override fun setSubtitleTrack(track: Track?) {}
    override fun stop() {}
}
