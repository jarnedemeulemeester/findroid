package dev.jdtech.jellyfin.viewmodels

import android.content.SharedPreferences
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.database.Server
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
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

    private val navigateToMain = MutableSharedFlow<Boolean>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    fun onNavigateToMain(scope: LifecycleCoroutineScope, collector: (Boolean) -> Unit) {
        scope.launch { navigateToMain.collect { collector(it) } }
    }

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
        val spEdit = sharedPreferences.edit()
        spEdit.putString("selectedServer", server.id)
        spEdit.apply()

        jellyfinApi.apply {
            api.baseUrl = server.address
            api.accessToken = server.accessToken
            userId = UUID.fromString(server.userId)
        }

        navigateToMain.tryEmit(true)
    }
}