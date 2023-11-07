package dev.jdtech.jellyfin.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.AppPreferences
import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.models.Server
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

    private val eventsChannel = Channel<ServerSelectEvent>()
    val eventsChannelFlow = eventsChannel.receiveAsFlow()

    sealed class UiState {
        data class Normal(val servers: List<Server>) : UiState()
        data object Loading : UiState()
        data class Error(val error: Exception) : UiState()
    }

    init {
        viewModelScope.launch {
            loadServers()
        }
    }

    private suspend fun loadServers() {
        val servers = database.getAllServersSync()
        _uiState.emit(UiState.Normal(servers))
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
            val serverWithAddressesAndUsers = database.getServerWithAddressesAndUsers(server.id) ?: return@launch
            val serverAddress = serverWithAddressesAndUsers.addresses.firstOrNull { it.id == server.currentServerAddressId } ?: return@launch
            val user = serverWithAddressesAndUsers.users.firstOrNull { it.id == server.currentUserId }

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

            eventsChannel.send(ServerSelectEvent.NavigateToHome)
        }
    }
}

sealed interface ServerSelectEvent {
    data object NavigateToHome : ServerSelectEvent
    data object NavigateToLogin : ServerSelectEvent
}
