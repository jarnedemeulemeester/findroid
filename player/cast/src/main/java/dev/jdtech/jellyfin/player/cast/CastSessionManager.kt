package dev.jdtech.jellyfin.player.cast

import dev.jdtech.jellyfin.player.cast.models.CastConnectionState
import dev.jdtech.jellyfin.player.cast.models.Device
import kotlinx.coroutines.flow.StateFlow
import androidx.mediarouter.media.MediaRouter

interface CastSessionManager {
    val isSupported: Boolean
    val connectionState: StateFlow<CastConnectionState>
    val availableDevices: StateFlow<List<Device>>
    val connectedDevice: StateFlow<Device?>

    fun init()
    fun updateDiscovery(flags: Int = MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY)
    fun connect(device: Device)
    fun disconnect()
    fun release()
}
