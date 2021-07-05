package dev.jdtech.jellyfin.viewmodels

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.database.Server
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ServerSelectViewModel
@Inject
constructor(
    private val application: Application,
    private val database: ServerDatabaseDao,
) : ViewModel() {

    private val _servers = database.getAllServers()
    val servers: LiveData<List<Server>> = _servers

    private val _navigateToMain = MutableLiveData<Boolean>()
    val navigateToMain: LiveData<Boolean> = _navigateToMain

    /**
     * Delete server from database
     *
     * @param server The server
     */
    fun deleteServer(server: Server) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                database.delete(server.id)
            }
        }
    }

    fun connectToServer(server: Server) {
        val jellyfinApi = JellyfinApi.newInstance(application, server.address).apply {
            api.accessToken = server.accessToken
            userId = UUID.fromString(server.userId)
        }

        viewModelScope.launch {
            postCapabilities(jellyfinApi)
        }


        _navigateToMain.value = true
    }

    fun doneNavigatingToMain() {
        _navigateToMain.value = false
    }

    private suspend fun postCapabilities(jellyfinApi: JellyfinApi) {
        withContext(Dispatchers.IO) {
            jellyfinApi.sessionApi.postCapabilities(
                playableMediaTypes = listOf("Video"),
                supportsMediaControl = false,
                supportsSync = false,
                supportsPersistentIdentifier = true
            )
        }
    }
}