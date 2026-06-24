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

enum class CastDeviceState {
    SEARCHING,
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

interface CastManager {
    /**
     * Indicates whether casting is supported in the current flavor (e.g., true in gms, false in libre).
     */
    val isSupported: Boolean

    /**
     * Flow of the current cast connection state.
     */
    val connectionState: StateFlow<CastConnectionState>

    /**
     * Flow containing the list of available casting devices found on the network.
     */
    val availableDevices: StateFlow<List<Device>>

    /**
     * Flow containing the currently connected device, or null if disconnected.
     */
    val connectedDevice: StateFlow<Device?>

    /**
     * Flow containing the currently playing item metadata and details.
     */
    val currentItem: StateFlow<PlayerItem?>

    /**
     * Signals when the receiver is playing an item that the local app doesn't have in cache.
     * Contains the ID of the item that needs to be restored from the server.
     */
    val restoringItemId: StateFlow<String?>

    /**
     * Flow tracking the playback state (playing/paused, position, duration).
     */
    val playbackState: StateFlow<CastPlaybackState>

    /**
     * Flow containing the active tracks currently selected on the remote receiver.
     */
    val activeTrackIds: StateFlow<List<Long>>

    /**
     * Flow containing available subtitle tracks for the current media.
     */
    val subtitleTracks: StateFlow<List<Track>>

    /**
     * Flow tracking the volume level of the cast session.
     */
    val volume: StateFlow<Float>

    /**
     * Initializes the CastManager, attaching state listeners and beginning device discovery.
     */
    fun init()

    /**
     * Initiates a connection to the specified target device.
     */
    fun connect(device: Device)

    /**
     * Disconnects from the currently connected device and clears session data.
     */
    fun disconnect()

    /**
     * Loads a specific item onto the remote receiver starting from the given position.
     */
    fun loadItem(item: PlayerItem, startPosition: Long)

    /**
     * Restores an item back into the local cache and state flow after being fetched from Jellyfin.
     * Usually called in response to [restoringItemId] emitting a non-null ID.
     */
    fun restoreItem(item: PlayerItem)

    /**
     * Appends an item to the end of the remote receiver's queue.
     */
    fun queueNextItem(item: PlayerItem)

    /**
     * Prepends an item or adds an item directly before the current item in the receiver's queue.
     */
    fun queuePreviousItem(item: PlayerItem)

    /**
     * Resumes playback on the remote receiver.
     */
    fun play()

    /**
     * Pauses playback on the remote receiver.
     */
    fun pause()

    /**
     * Seeks to a specific millisecond position in the current media.
     */
    fun seekTo(position: Long)

    /**
     * Skips to the next item in the receiver's queue.
     */
    fun seekToNext()

    /**
     * Skips to the previous item in the receiver's queue.
     */
    fun seekToPrevious()

    /**
     * Changes the playback volume of the remote receiver.
     */
    fun setVolume(volume: Float)

    /**
     * Modifies the active audio track on the remote receiver.
     */
    fun setAudioTrack(track: Track?)

    /**
     * Modifies the active subtitle track on the remote receiver.
     */
    fun setSubtitleTrack(track: Track?)

    /**
     * Stops playback and clears the remote receiver's queue.
     */
    fun stop()
}
