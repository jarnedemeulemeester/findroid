
package dev.jdtech.jellyfin.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ConnectivityState {
    Online,
    Offline,
    Unknown,
}

class ConnectivityMonitor(context: Context) {
    private val connectivityManager =
        context.applicationContext.getSystemService(ConnectivityManager::class.java)

    private val _state = MutableStateFlow(currentState())
    val state: StateFlow<ConnectivityState> = _state.asStateFlow()
    
    private val networkCallback =
        object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                updateState(ConnectivityState.Online)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                capabilities: NetworkCapabilities,
            ) {
                updateState(stateFor(capabilities))
            }

            override fun onLost(network: Network) {
                updateState(ConnectivityState.Offline)
            }
        }

    init {
        connectivityManager.registerDefaultNetworkCallback(networkCallback)
    }

    private fun currentState(): ConnectivityState {
        val network = connectivityManager.activeNetwork ?: return ConnectivityState.Offline
        val capabilities = connectivityManager.getNetworkCapabilities(network)
            ?: return ConnectivityState.Unknown
        return stateFor(capabilities)
    }


    private fun stateFor(capabilities: NetworkCapabilities): ConnectivityState {
        return if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            ConnectivityState.Online
        } else {
            ConnectivityState.Offline
        }
    }

    fun updateState(newState: ConnectivityState) {
        _state.value = newState
    }
}