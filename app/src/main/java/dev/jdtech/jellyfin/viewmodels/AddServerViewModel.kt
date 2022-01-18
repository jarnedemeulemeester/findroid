package dev.jdtech.jellyfin.viewmodels

import android.content.res.Resources
import android.widget.Toast
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.BaseApplication
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.database.Server
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
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
    private val resources: Resources = application.resources

    private val uiState = MutableStateFlow<UiState>(UiState.Normal)

    private val navigateToLogin = MutableSharedFlow<Boolean>()

    sealed class UiState {
        object Normal : UiState()
        object Loading : UiState()
        data class Error(val message: String) : UiState()
    }

    fun onUiState(scope: LifecycleCoroutineScope, collector: (UiState) -> Unit) {
        scope.launch { uiState.collect { collector(it) } }
    }

    fun onNavigateToLogin(scope: LifecycleCoroutineScope, collector: (Boolean) -> Unit) {
        scope.launch { navigateToLogin.collect { collector(it) } }
    }

    /**
     * Run multiple check on the server before continuing:
     *
     * - Connect to server and check if it is a Jellyfin server
     * - Check if server is not already in Database
     *
     * @param inputValue Can be an ip address or hostname
     */
    fun checkServer(inputValue: String) {

        viewModelScope.launch {
            uiState.emit(UiState.Loading)

            try {
                // Check if input value is not empty
                if (inputValue.isBlank()) {
                    throw Exception(resources.getString(R.string.add_server_error_empty_address))
                }

                val candidates = jellyfinApi.jellyfin.discovery.getAddressCandidates(inputValue)
                val recommended = jellyfinApi.jellyfin.discovery.getRecommendedServers(
                    candidates,
                    RecommendedServerInfoScore.OK
                )

                val greatServers = mutableListOf<RecommendedServerInfo>()
                val goodServers = mutableListOf<RecommendedServerInfo>()
                val okServers = mutableListOf<RecommendedServerInfo>()

                recommended
                    .onCompletion {
                        when {
                            greatServers.isNotEmpty() -> {
                                connectToServer(greatServers.first())
                            }
                            goodServers.isNotEmpty() -> {
                                val issuesString = createIssuesString(goodServers.first())
                                Toast.makeText(
                                    application,
                                    issuesString,
                                    Toast.LENGTH_LONG
                                ).show()
                                connectToServer(goodServers.first())
                            }
                            okServers.isNotEmpty() -> {
                                val okServer = okServers.first()
                                val issuesString = createIssuesString(okServer)
                                throw Exception(issuesString)
                            }
                            else -> {
                                throw Exception(resources.getString(R.string.add_server_error_not_found))
                            }
                        }
                    }
                    .collect { recommendedServerInfo ->
                        when (recommendedServerInfo.score) {
                            RecommendedServerInfoScore.GREAT -> greatServers.add(recommendedServerInfo)
                            RecommendedServerInfoScore.GOOD -> goodServers.add(recommendedServerInfo)
                            RecommendedServerInfoScore.OK -> okServers.add(recommendedServerInfo)
                            RecommendedServerInfoScore.BAD -> Unit
                        }
                    }
            } catch (e: Exception) {
                uiState.emit(
                    UiState.Error(
                        e.message ?: resources.getString(R.string.unknown_error)
                    )
                )
            }
        }
    }

    private suspend fun connectToServer(recommendedServerInfo: RecommendedServerInfo) {
        val serverId = recommendedServerInfo.systemInfo.getOrNull()?.id
            ?: throw Exception(resources.getString(R.string.add_server_error_no_id))

        Timber.d("Connecting to server: $serverId")

        if (serverAlreadyInDatabase(serverId)) {
            throw Exception(resources.getString(R.string.add_server_error_already_added))
        }

        jellyfinApi.apply {
            api.baseUrl = recommendedServerInfo.address
            api.accessToken = null
        }

        uiState.emit(UiState.Normal)
        navigateToLogin.emit(true)
    }

    /**
     * Create a presentable string of issues with a server
     *
     * @param server The server with issues
     * @return A presentable string of issues separated with \n
     */
    private fun createIssuesString(server: RecommendedServerInfo): String {
        return server.issues.joinToString("\n") {
            when (it) {
                is RecommendedServerIssue.OutdatedServerVersion -> {
                    String.format(
                        resources.getString(R.string.add_server_error_outdated),
                        it.version
                    )
                }
                is RecommendedServerIssue.InvalidProductName -> {
                    String.format(
                        resources.getString(R.string.add_server_error_not_jellyfin),
                        it.productName
                    )
                }
                is RecommendedServerIssue.UnsupportedServerVersion -> {
                    String.format(
                        resources.getString(R.string.add_server_error_version),
                        it.version
                    )
                }
                is RecommendedServerIssue.SlowResponse -> {
                    String.format(
                        resources.getString(R.string.add_server_error_slow),
                        it.responseTime
                    )
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
    private suspend fun serverAlreadyInDatabase(id: String): Boolean {
        val server: Server?
        withContext(Dispatchers.IO) {
            server = database.get(id)
        }
        if (server != null) return true
        return false
    }
}