package dev.jdtech.jellyfin.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.AppPreferences
import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.models.Server
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
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
    val servers = database.getAllServers()

    private val _navigateToMain = MutableSharedFlow<Boolean>()
    val navigateToMain = _navigateToMain.asSharedFlow()

    /**
     * Delete server from database
     *
     * @param server The server
     */
    fun deleteServer(server: Server) {
        viewModelScope.launch(Dispatchers.IO) {
            database.delete(server.id)
        }
    }

    fun connectToServer(server: Server) {
        viewModelScope.launch {
            val serverWithAddressesAndUsers = database.getServerWithAddressesAndUsers(server.id)!!
            val serverAddress = serverWithAddressesAndUsers.addresses.firstOrNull { it.id == server.currentServerAddressId } ?: return@launch
            val user = serverWithAddressesAndUsers.users.firstOrNull { it.id == server.currentUserId } ?: return@launch

            jellyfinApi.apply {
                api.baseUrl = serverAddress.address
                api.accessToken = user.accessToken
                userId = user.id
            }

            appPreferences.currentServer = server.id

            _navigateToMain.emit(true)
        }
    }
}
