package dev.jdtech.jellyfin.setup.presentation.servers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.AppPreferences
import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.setup.domain.SetupRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ServersViewModel
@Inject
constructor(
    private val repository: SetupRepository,
    private val database: ServerDatabaseDao,
    private val jellyfinApi: JellyfinApi,
    private val appPreferences: AppPreferences,
) : ViewModel() {
    private val _state = MutableStateFlow(ServersState())
    val state = _state.asStateFlow()

    private val eventsChannel = Channel<ServersEvent>()
    val events = eventsChannel.receiveAsFlow()

    fun loadServers() {
        viewModelScope.launch {
            val servers = repository.getServers()
            _state.emit(
                ServersState(servers = servers),
            )
        }
    }

    private fun connectToServer(serverId: String) {
        viewModelScope.launch {
            val serverWithAddressAndUser = database.getServerWithAddressAndUser(serverId) ?: return@launch
            val serverAddress = serverWithAddressAndUser.address ?: return@launch

            jellyfinApi.apply {
                api.update(
                    baseUrl = serverAddress.address,
                    accessToken = null,
                )
                userId = null
            }

            appPreferences.currentServer = serverId

            eventsChannel.send(ServersEvent.NavigateToLogin)
        }
    }

    private fun deleteServer(serverId: String) {
        viewModelScope.launch {
            repository.deleteServer(serverId)
            loadServers()
        }
    }

    fun onAction(action: ServersAction) {
        when (action) {
            is ServersAction.OnServerClick -> {
                connectToServer(action.serverId)
            }
            is ServersAction.DeleteServer -> {
                deleteServer(action.serverId)
            }
            else -> Unit
        }
    }
}
