package dev.jdtech.jellyfin.player.cast

import dev.jdtech.jellyfin.player.cast.models.CastConnectionState
import dev.jdtech.jellyfin.player.cast.models.Device
import kotlinx.coroutines.flow.StateFlow

interface CastSessionManager {
    val isSupported: Boolean
    val connectionState: StateFlow<CastConnectionState>
    val availableDevices: StateFlow<List<Device>>
    val connectedDevice: StateFlow<Device?>

    fun init()
    fun connect(device: Device)
    fun disconnect()
}
