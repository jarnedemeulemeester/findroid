package dev.jdtech.jellyfin.player.cast

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.jdtech.jellyfin.player.cast.models.CastConnectionState
import dev.jdtech.jellyfin.player.cast.models.Device
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class CastSessionManagerImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : CastSessionManager {
    override val isSupported = false

    private val _connectionState = MutableStateFlow(CastConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<CastConnectionState> = _connectionState.asStateFlow()

    private val _availableDevices = MutableStateFlow<List<Device>>(emptyList())
    override val availableDevices: StateFlow<List<Device>> = _availableDevices.asStateFlow()

    private val _connectedDevice = MutableStateFlow<Device?>(null)
    override val connectedDevice: StateFlow<Device?> = _connectedDevice.asStateFlow()

    override val currentSession = MutableStateFlow<Any?>(null)
    override val remoteMediaClient = MutableStateFlow<Any?>(null)

    override fun init() {}
    override fun updateDiscovery(flags: Int) {}
    override fun connect(device: Device) {}
    override fun disconnect() {}
}
