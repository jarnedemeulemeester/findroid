package dev.jdtech.jellyfin.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.database.Server
import dev.jdtech.jellyfin.database.ServerDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Exception

class AddServerViewModel(val application: Application) : ViewModel() {
    private val database = ServerDatabase.getInstance(application).serverDatabaseDao

    private val _navigateToLogin = MutableLiveData<Boolean>()
    val navigateToLogin: LiveData<Boolean> = _navigateToLogin

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    /**
     * Run multiple check on the server before continuing:
     *
     * - Connect to server and check if it is a Jellyfin server
     * - Check if server is not already in Database
     */
    fun checkServer(baseUrl: String) {
        _error.value = null

        viewModelScope.launch {
            val jellyfinApi = JellyfinApi.newInstance(application, baseUrl)
            try {
                val publicSystemInfo by jellyfinApi.systemApi.getPublicSystemInfo()
                Log.i("AddServerViewModel", "Remote server: ${publicSystemInfo.id}")

                if (serverAlreadyInDatabase(publicSystemInfo.id)) {
                    _error.value = "Server already added"
                    _navigateToLogin.value = false
                } else {
                    _error.value = null
                    _navigateToLogin.value = true
                }
            } catch (e: Exception) {
                Log.e("AddServerViewModel", "${e.message}")
                _error.value = e.message
                _navigateToLogin.value = false
            }
        }
    }

    /**
     * Check if server is already in database using server ID
     *
     * @param id Server ID
     * @return True if server is already in database
     */
    private suspend fun serverAlreadyInDatabase(id: String?): Boolean {
        val servers: List<Server>
        withContext(Dispatchers.IO) {
            servers = database.getAllServersSync()
        }
        for (server in servers) {
            Log.i("AddServerViewModel", "Database server: ${server.id}")
            if (server.id == id) {
                Log.i("AddServerViewModel", "Server already in the database")
                return true
            }
        }
        return false
    }

    fun onNavigateToLoginDone() {
        _navigateToLogin.value = false
    }
}