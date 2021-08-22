package dev.jdtech.jellyfin.viewmodels

import android.content.SharedPreferences
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
import timber.log.Timber
import java.util.*
import javax.inject.Inject

@HiltViewModel
class MainViewModel
@Inject
constructor(
    private val sharedPreferences: SharedPreferences,
    private val database: ServerDatabaseDao,
    private val jellyfinApi: JellyfinApi,
) : ViewModel() {

    private val _doneLoading = MutableLiveData<Boolean>()
    val doneLoading: LiveData<Boolean> = _doneLoading

    private val _navigateToAddServer = MutableLiveData<Boolean>()
    val navigateToAddServer: LiveData<Boolean> = _navigateToAddServer

    init {
        Timber.d("Start Main")
        viewModelScope.launch {
            val servers: List<Server>
            withContext(Dispatchers.IO) {
                servers = database.getAllServersSync()
            }
            if (servers.isEmpty()) {
                _navigateToAddServer.value = true
            } else {
                val serverId = sharedPreferences.getString("selectedServer", null)
                val selectedServer = servers.find { server -> server.id == serverId }
                Timber.d("Selected server: $selectedServer")
                if (selectedServer != null) {
                    jellyfinApi.apply {
                        api.baseUrl = selectedServer.address
                        api.accessToken = selectedServer.accessToken
                        userId = UUID.fromString(selectedServer.userId)
                    }
                    Timber.d("Finish Main")
                }
                _doneLoading.value = true
            }
        }
    }

    fun doneNavigateToAddServer() {
        _navigateToAddServer.value = false
    }
}