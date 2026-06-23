package dev.jdtech.jellyfin.player.cast

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.jdtech.jellyfin.player.core.domain.models.PlayerItem
import dev.jdtech.jellyfin.player.core.domain.models.Track
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class CastManagerImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : CastManager {

    override val isSupported = false

    private val _connectionState = MutableStateFlow(CastConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<CastConnectionState> = _connectionState.asStateFlow()

    private val _availableDevices = MutableStateFlow<List<CastDevice>>(emptyList())
    override val availableDevices: StateFlow<List<CastDevice>> = _availableDevices.asStateFlow()

    private val _connectedDevice = MutableStateFlow<CastDevice?>(null)
    override val connectedDevice: StateFlow<CastDevice?> = _connectedDevice.asStateFlow()

    private val _currentItem = MutableStateFlow<PlayerItem?>(null)
    override val currentItem: StateFlow<PlayerItem?> = _currentItem.asStateFlow()

    private val _playbackState = MutableStateFlow(CastPlaybackState())
    override val playbackState: StateFlow<CastPlaybackState> = _playbackState.asStateFlow()

    private val _audioTracks = MutableStateFlow<List<Track>>(emptyList())
    override val audioTracks: StateFlow<List<Track>> = _audioTracks.asStateFlow()

    private val _subtitleTracks = MutableStateFlow<List<Track>>(emptyList())
    override val subtitleTracks: StateFlow<List<Track>> = _subtitleTracks.asStateFlow()

    private val _volume = MutableStateFlow(1f)
    override val volume: StateFlow<Float> = _volume.asStateFlow()

    override fun init() {}

    override fun connect(device: CastDevice) {}

    override fun disconnect() {}

    override fun loadItem(item: PlayerItem, startPosition: Long) {}

    override fun queueNextItem(item: PlayerItem) {}

    override fun queuePreviousItem(item: PlayerItem) {}

    override fun play() {}

    override fun pause() {}

    override fun seekTo(position: Long) {}

    override fun seekToNext() {}

    override fun seekToPrevious() {}

    override fun setVolume(volume: Float) {
        _volume.value = volume
    }

    override fun setAudioTrack(track: Track?) {}

    override fun setSubtitleTrack(track: Track?) {}

    override fun stop() {}
}
