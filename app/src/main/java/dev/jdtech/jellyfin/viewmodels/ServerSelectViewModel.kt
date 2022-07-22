package dev.jdtech.jellyfin.viewmodels

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.database.Server
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

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
            val spEdit = sharedPreferences.edit()
            spEdit.putString("selectedServer", server.id)
            spEdit.apply()

            jellyfinApi.apply {
                api.baseUrl = server.address
                api.accessToken = server.accessToken
                userId = UUID.fromString(server.userId)
            }

            _navigateToMain.emit(true)
        }
    }
}