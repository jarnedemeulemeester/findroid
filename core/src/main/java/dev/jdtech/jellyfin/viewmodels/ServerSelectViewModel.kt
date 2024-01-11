package dev.jdtech.jellyfin.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.AppPreferences
import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.models.DiscoveredServer
import dev.jdtech.jellyfin.models.Server
import dev.jdtech.jellyfin.models.UiText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ServerSelectViewModel
@Inject
constructor(
    private val jellyfinApi: JellyfinApi,
    private val database: ServerDatabaseDao,
    private val appPreferences: AppPreferences,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState = _uiState.asStateFlow()
    private val _discoveredServersState = MutableStateFlow<DiscoveredServersState>(DiscoveredServersState.Loading)
    val discoveredServersState = _discoveredServersState.asStateFlow()

    var currentServerId: String? = appPreferences.currentServer
    private val eventsChannel = Channel<ServerSelectEvent>()
    val eventsChannelFlow = eventsChannel.receiveAsFlow()

    // TODO states may need to be merged / cleaned up
    sealed class UiState {
        data class Normal(val servers: List<Server>) : UiState()
        data object Loading : UiState()
        data class Error(val message: Collection<UiText>) : UiState()
    }

    sealed class DiscoveredServersState {
        data object Loading : DiscoveredServersState()
        data class Servers(val servers: List<DiscoveredServer>) : DiscoveredServersState()
    }

    private val discoveredServers = mutableListOf<DiscoveredServer>()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            loadServers()
            discoverServers()
        }
    }

    /**
     * Get Jellyfin servers stored in the database and emit them
     */
    private suspend fun loadServers() {
        val servers = database.getAllServersSync()
        _uiState.emit(UiState.Normal(servers))
    }

    /**
     * Discover Jellyfin servers and emit them
     */
    private suspend fun discoverServers() {
        val servers = jellyfinApi.jellyfin.discovery.discoverLocalServers()
        servers.collect { serverDiscoveryInfo ->
            discoveredServers.add(
                DiscoveredServer(
                    serverDiscoveryInfo.id,
                    serverDiscoveryInfo.name,
                    serverDiscoveryInfo.address,
                ),
            )
            _discoveredServersState.emit(DiscoveredServersState.Servers(ArrayList(discoveredServers)))
        }
    }

    /**
     * Delete server from database
     *
     * @param server The server
     */
    fun deleteServer(server: Server) {
        viewModelScope.launch(Dispatchers.IO) {
            database.delete(server.id)
            loadServers()
        }
    }

    fun connectToServer(server: Server) {
        viewModelScope.launch {
            val serverWithAddressAndUser = database.getServerWithAddressAndUser(server.id) ?: return@launch
            val serverAddress = serverWithAddressAndUser.address ?: return@launch
            val user = serverWithAddressAndUser.user

            // If server has no selected user, navigate to login fragment
            if (user == null) {
                jellyfinApi.apply {
                    api.baseUrl = serverAddress.address
                    api.accessToken = null
                    userId = null
                }
                appPreferences.currentServer = server.id
                eventsChannel.send(ServerSelectEvent.NavigateToLogin)
                return@launch
            }

            jellyfinApi.apply {
                api.baseUrl = serverAddress.address
                api.accessToken = user.accessToken
                userId = user.id
            }

            appPreferences.currentServer = server.id
            currentServerId = server.id

            eventsChannel.send(ServerSelectEvent.NavigateToHome)
        }
    }
}

sealed interface ServerSelectEvent {
    data object NavigateToHome : ServerSelectEvent
    data object NavigateToLogin : ServerSelectEvent
}
