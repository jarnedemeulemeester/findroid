package dev.jdtech.jellyfin.serverselect

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dev.jdtech.jellyfin.database.Server
import java.util.*

class ServerSelectViewModel : ViewModel() {
    private val _servers = MutableLiveData<List<Server>>()
    val servers: LiveData<List<Server>>
        get() = _servers

    init {
        val server = Server(UUID.randomUUID(), "JDTech", "https://jellyfin.jdtech.dev")
        val demoServer = Server(UUID.randomUUID(), "Demo", "https://demo.jellyfin.org")
        _servers.value = listOf(server, demoServer)
    }
}