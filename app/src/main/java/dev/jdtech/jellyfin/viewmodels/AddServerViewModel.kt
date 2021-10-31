package dev.jdtech.jellyfin.viewmodels

import android.content.res.Resources
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.BaseApplication
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.database.Server
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.discovery.RecommendedServerInfo
import org.jellyfin.sdk.discovery.RecommendedServerInfoScore
import org.jellyfin.sdk.discovery.RecommendedServerIssue
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AddServerViewModel
@Inject
constructor(
    private val application: BaseApplication,
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
    fun checkServer(inputValue: String, resources: Resources) {
        _error.value = null

        viewModelScope.launch {
            try {
                val candidates = jellyfinApi.jellyfin.discovery.getAddressCandidates(inputValue)
                val recommended = jellyfinApi.jellyfin.discovery.getRecommendedServers(
                    candidates,
                    RecommendedServerInfoScore.OK
                )

                // Check if any servers have been found
                if (recommended.toList().isNullOrEmpty()) {
                    throw Exception(resources.getString(R.string.add_server_error_not_found))
                }

                // Create separate flow of great, good and ok servers.
                val greatServers =
                    recommended.filter { it.score == RecommendedServerInfoScore.GREAT }
                val goodServers = recommended.filter { it.score == RecommendedServerInfoScore.GOOD }
                val okServers = recommended.filter { it.score == RecommendedServerInfoScore.OK }

                // Only allow connecting to great and good servers. Show toast of issues if good server
                val recommendedServer = if (greatServers.toList().isNotEmpty()) {
                    greatServers.first()
                } else if (goodServers.toList().isNotEmpty()) {
                    val issuesString = createIssuesString(goodServers.first(), resources)
                    Toast.makeText(
                        application,
                        issuesString,
                        Toast.LENGTH_LONG
                    ).show()
                    goodServers.first()
                } else {
                    val okServer = okServers.first()
                    val issuesString = createIssuesString(okServer, resources)
                    throw Exception(issuesString)
                }

                jellyfinApi.apply {
                    api.baseUrl = recommendedServer.address
                    api.accessToken = null
                }

                Timber.d("Remote server: ${recommendedServer.systemInfo.getOrNull()?.id}")

                if (serverAlreadyInDatabase(recommendedServer.systemInfo.getOrNull()?.id)) {
                    _error.value = resources.getString(R.string.add_server_error_already_added)
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
     * Create a presentable string of issues with a server
     *
     * @param server The server with issues
     * @return A presentable string of issues separated with \n
     */
    private fun createIssuesString(server: RecommendedServerInfo, resources: Resources): String {
        return server.issues.joinToString("\n") {
            when (it) {
                is RecommendedServerIssue.OutdatedServerVersion -> {
                    String.format(resources.getString(R.string.add_server_error_outdated), it.version)
                }
                is RecommendedServerIssue.InvalidProductName -> {
                    String.format(resources.getString(R.string.add_server_error_not_jellyfin), it.productName)
                }
                is RecommendedServerIssue.UnsupportedServerVersion -> {
                    String.format(resources.getString(R.string.add_server_error_version), it.version)
                }
                is RecommendedServerIssue.SlowResponse -> {
                    String.format(resources.getString(R.string.add_server_error_slow), it.responseTime)
                }
                else -> {
                    resources.getString(R.string.unknown_error)
                }
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