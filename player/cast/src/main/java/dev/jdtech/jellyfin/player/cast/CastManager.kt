package dev.jdtech.jellyfin.player.cast

import dev.jdtech.jellyfin.player.core.domain.models.PlayerItem
import dev.jdtech.jellyfin.player.core.domain.models.Track
import kotlinx.coroutines.flow.StateFlow

enum class CastConnectionState {
    DISCONNECTED,
    AVAILABLE,
    CONNECTING,
    CONNECTED
}

data class CastPlaybackState(
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L
)

data class CastDevice(val id: String, val name: String)

interface CastManager {
    val isSupported: Boolean
    val connectionState: StateFlow<CastConnectionState>
    val availableDevices: StateFlow<List<CastDevice>>
    val connectedDevice: StateFlow<CastDevice?>
    val currentItem: StateFlow<PlayerItem?>
    val playbackState: StateFlow<CastPlaybackState>
    val audioTracks: StateFlow<List<Track>>
    val subtitleTracks: StateFlow<List<Track>>
    val volume: StateFlow<Float>

    fun init()
    fun connect(device: CastDevice)
    fun disconnect()
    fun loadItem(item: PlayerItem, startPosition: Long)
    fun queueNextItem(item: PlayerItem)
    fun queuePreviousItem(item: PlayerItem)
    fun play()
    fun pause()
    fun seekTo(position: Long)
    fun seekToNext()
    fun seekToPrevious()
    fun setVolume(volume: Float)
    fun setAudioTrack(track: Track?)
    fun setSubtitleTrack(track: Track?)
    fun stop()
}
