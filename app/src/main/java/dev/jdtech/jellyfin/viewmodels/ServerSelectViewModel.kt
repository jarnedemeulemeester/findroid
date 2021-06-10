package dev.jdtech.jellyfin.viewmodels

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jdtech.jellyfin.database.Server
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ServerSelectViewModel(
    val database: ServerDatabaseDao,
    val application: Application,
) : ViewModel() {
    private val _servers = database.getAllServers()
    val servers: LiveData<List<Server>>
        get() = _servers

    fun deleteServer(server: Server) {
        viewModelScope.launch {
            delete(server)
        }
    }

    private suspend fun delete(server: Server) {
        withContext(Dispatchers.IO) {
            database.delete(server.id)
        }
    }
}