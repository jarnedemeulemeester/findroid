package dev.jdtech.jellyfin.serverselect

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
) : ViewModel() {
    private val _servers = database.getAllServers()
    val servers: LiveData<List<Server>>
        get() = _servers

    init {
        val demoServer = Server("0", "Demo", "https://demo.jellyfin.org", "0", "demo", "")

        viewModelScope.launch {
            clearDatabase()
            insert(demoServer)
        }
    }

    private suspend fun insert(server: Server) {
        withContext(Dispatchers.IO) {
            database.insert(server)
        }
    }

    private suspend fun delete(server: Server) {
        withContext(Dispatchers.IO) {
            database.delete(server.id)
        }
    }

    private suspend fun clearDatabase() {
        withContext(Dispatchers.IO) {
            database.clear()
        }
    }
}