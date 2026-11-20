package dev.jdtech.jellyfin.setup.presentation.addresses

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.core.domain.ServerAddressPinger
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.models.ServerAddress
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class ServerAddressesViewModel
@Inject
constructor(
    val application: Application,
    private val database: ServerDatabaseDao,
    private val jellyfinApi: JellyfinApi,
    private val pinger: ServerAddressPinger,
    private val appPreferences: AppPreferences,
) : ViewModel() {
    private val _state = MutableStateFlow(ServerAddressesState())
    val state = _state.asStateFlow()

    private var currentServerId: String = ""

    fun loadAddresses(serverId: String) {
        currentServerId = serverId
        viewModelScope.launch {
            try {
                val serverWithAddresses = database.getServerWithAddresses(serverId)
                _state.emit(
                    ServerAddressesState(
                        addresses = serverWithAddresses.addresses,
                        currentAddressId = serverWithAddresses.server.currentServerAddressId,
                        latencies = _state.value.latencies,
                        autoSelectFastest =
                            appPreferences.getValue(appPreferences.autoSelectFastestServerAddress),
                    )
                )
                measureLatency()
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    fun measureLatency() {
        val addresses = _state.value.addresses
        if (addresses.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(isMeasuring = true) }
            try {
                val latencies = pinger.pingAll(addresses.map { address -> address.address })
                _state.update { state ->
                    state.copy(
                        latencies =
                            state.addresses.associate { address ->
                                address.id to latencies[address.address]
                            },
                        isMeasuring = false,
                    )
                }
            } catch (e: Exception) {
                Timber.e(e)
                _state.update { it.copy(isMeasuring = false) }
            }
        }
    }

    fun deleteAddress(addressId: UUID) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentAddress = database.getServerCurrentAddress(currentServerId)
            if (addressId == currentAddress?.id) {
                Timber.e("You cannot delete the current address")
                return@launch
            }
            database.deleteServerAddress(addressId)
            loadAddresses(currentServerId)
        }
    }

    fun setCurrentAddress(addressId: UUID) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val address = database.getAddress(addressId)
                database.updateServerCurrentAddress(currentServerId, addressId)
                jellyfinApi.api.update(baseUrl = address.address)
                loadAddresses(currentServerId)
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    fun addAddress(address: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val jellyfinApi = JellyfinApi(application.applicationContext)
                jellyfinApi.api.update(baseUrl = address)
                val systemInfo by jellyfinApi.systemApi.getPublicSystemInfo()
                if (systemInfo.id != currentServerId) {
                    return@launch
                }
                val serverAddress = ServerAddress(UUID.randomUUID(), currentServerId, address)
                database.insertServerAddress(serverAddress)
                loadAddresses(currentServerId)
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    fun setAutoSelectFastest(enabled: Boolean) {
        appPreferences.setValue(appPreferences.autoSelectFastestServerAddress, enabled)
        _state.update { it.copy(autoSelectFastest = enabled) }
    }

    fun onAction(action: ServerAddressesAction) {
        when (action) {
            is ServerAddressesAction.AddAddress -> {
                addAddress(action.address)
            }
            is ServerAddressesAction.DeleteAddress -> {
                deleteAddress(action.addressId)
            }
            is ServerAddressesAction.OnServerClick -> {
                setCurrentAddress(action.addressId)
            }
            is ServerAddressesAction.MeasureLatency -> {
                measureLatency()
            }
            is ServerAddressesAction.SetAutoSelectFastest -> {
                setAutoSelectFastest(action.enabled)
            }
            else -> Unit
        }
    }
}
