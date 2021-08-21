package dev.jdtech.jellyfin.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.database.Server
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.discovery.RecommendedServerInfo
import org.jellyfin.sdk.discovery.RecommendedServerInfoScore
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AddServerViewModel
@Inject
constructor(
    private val jellyfinApi: JellyfinApi,
    private val database: ServerDatabaseDao
) : ViewModel() {

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
    fun checkServer(inputValue: String) {
        _error.value = null

        viewModelScope.launch {
            try {
                val candidates = jellyfinApi.jellyfin.discovery.getAddressCandidates(inputValue)
                val recommended = jellyfinApi.jellyfin.discovery.getRecommendedServers(
                    candidates,
                    RecommendedServerInfoScore.GOOD
                )
                val recommendedServer: RecommendedServerInfo

                try {
                    recommendedServer = recommended.first()
                } catch (e: NoSuchElementException) {
                    throw Exception("Server not found")
                }

                jellyfinApi.apply {
                    api.baseUrl = recommendedServer.address
                    api.accessToken = null
                }

                Timber.d("Remote server: ${recommendedServer.systemInfo?.id}")

                if (serverAlreadyInDatabase(recommendedServer.systemInfo?.id)) {
                    _error.value = "Server already added"
                    _navigateToLogin.value = false
                } else {
                    _error.value = null
                    _navigateToLogin.value = true
                }
            } catch (e: Exception) {
                Timber.e(e)
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
            Timber.d("Database server: ${server.id}")
            if (server.id == id) {
                Timber.w("Server already in the database")
                return true
            }
        }
        return false
    }

    fun onNavigateToLoginDone() {
        _navigateToLogin.value = false
    }
}