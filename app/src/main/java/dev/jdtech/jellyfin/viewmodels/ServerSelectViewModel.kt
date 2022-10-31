package dev.jdtech.jellyfin.viewmodels

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.models.Server
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ServerSelectViewModel
@Inject
constructor(
    private val sharedPreferences: SharedPreferences,
    private val jellyfinApi: JellyfinApi,
    private val database: ServerDatabaseDao,
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

            val spEdit = sharedPreferences.edit()
            spEdit.putString("selectedServer", server.id)
            spEdit.apply()

            _navigateToMain.emit(true)
        }
    }
}
