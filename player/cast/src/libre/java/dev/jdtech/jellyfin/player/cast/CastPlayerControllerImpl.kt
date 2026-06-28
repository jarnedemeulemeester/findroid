package dev.jdtech.jellyfin.player.cast

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.jdtech.jellyfin.player.core.domain.models.PlayerItem
import dev.jdtech.jellyfin.player.core.domain.models.Track
import dev.jdtech.jellyfin.player.cast.models.CastPlayerState
import org.jellyfin.sdk.model.UUID
import org.jellyfin.sdk.model.api.PlaybackInfoResponse
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class CastPlayerControllerImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : CastPlayerController {

    private val _currentItem = MutableStateFlow<PlayerItem?>(null)
    override val currentItem: StateFlow<PlayerItem?> = _currentItem.asStateFlow()

    private val _playbackState = MutableStateFlow(CastPlayerState())
    override val playbackState: StateFlow<CastPlayerState> = _playbackState.asStateFlow()

    private val _subtitleTracks = MutableStateFlow<List<Track>>(emptyList())
    override val subtitleTracks: StateFlow<List<Track>> = _subtitleTracks.asStateFlow()

    private val _audioTracks = MutableStateFlow<List<Track>>(emptyList())
    override val audioTracks: StateFlow<List<Track>> = _audioTracks.asStateFlow()

    private val _volume = MutableStateFlow(1f)
    override val volume: StateFlow<Float> = _volume.asStateFlow()

    override fun playItem(itemId: UUID, itemKind: String, startFromBeginning: Boolean) {}
    override fun play() {}
    override fun pause() {}
    override fun seekTo(position: Long) {}
    override fun seekToNext() {}
    override fun seekToPrevious() {}
    override fun setVolume(volume: Float) {
        _volume.value = volume
    }
    override fun setAudioTrack(track: Track?, itemId: UUID?) {}
    override fun setSubtitleTrack(track: Track?) {}
    override fun stop() {}
}
