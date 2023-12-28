package dev.jdtech.jellyfin.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.AppPreferences
import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.core.R
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.models.DiscoveredServer
import dev.jdtech.jellyfin.models.ExceptionUiText
import dev.jdtech.jellyfin.models.ExceptionUiTexts
import dev.jdtech.jellyfin.models.Server
import dev.jdtech.jellyfin.models.ServerAddress
import dev.jdtech.jellyfin.models.UiText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.discovery.RecommendedServerInfo
import org.jellyfin.sdk.discovery.RecommendedServerInfoScore
import org.jellyfin.sdk.discovery.RecommendedServerIssue
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AddServerViewModel
@Inject
constructor(
    private val appPreferences: AppPreferences,
    private val jellyfinApi: JellyfinApi,
    private val database: ServerDatabaseDao,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Normal)
    val uiState = _uiState.asStateFlow()
    private val _discoveredServersState = MutableStateFlow<DiscoveredServersState>(DiscoveredServersState.Loading)
    val discoveredServersState = _discoveredServersState.asStateFlow()

    private val eventsChannel = Channel<AddServerEvent>()
    val eventsChannelFlow = eventsChannel.receiveAsFlow()

    private val discoveredServers = mutableListOf<DiscoveredServer>()
    private var serverFound = false

    sealed class UiState {
        data object Normal : UiState()
        data object Loading : UiState()
        data class Error(val message: Collection<UiText>) : UiState()
    }

    sealed class DiscoveredServersState {
        data object Loading : DiscoveredServersState()
        data class Servers(val servers: List<DiscoveredServer>) : DiscoveredServersState()
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val servers = jellyfinApi.jellyfin.discovery.discoverLocalServers()
            servers.collect { serverDiscoveryInfo ->
                discoveredServers.add(
                    DiscoveredServer(
                        serverDiscoveryInfo.id,
                        serverDiscoveryInfo.name,
                        serverDiscoveryInfo.address,
                    ),
                )
                _discoveredServersState.emit(
                    DiscoveredServersState.Servers(ArrayList(discoveredServers)),
                )
            }
        }
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
            _uiState.emit(UiState.Loading)

            try {
                // Check if input value is not empty
                if (inputValue.isBlank()) {
                    throw ExceptionUiText(UiText.StringResource(R.string.add_server_error_empty_address))
                }

                val candidates = jellyfinApi.jellyfin.discovery.getAddressCandidates(inputValue)
                val recommended = jellyfinApi.jellyfin.discovery.getRecommendedServers(
                    candidates,
                    RecommendedServerInfoScore.OK,
                )

                val goodServers = mutableListOf<RecommendedServerInfo>()
                val okServers = mutableListOf<RecommendedServerInfo>()

                for (recommendedServerInfo in recommended) {
                    when (recommendedServerInfo.score) {
                        RecommendedServerInfoScore.GREAT -> {
                            serverFound = true
                            connectToServer(recommendedServerInfo)
                        }
                        RecommendedServerInfoScore.GOOD -> goodServers.add(recommendedServerInfo)
                        RecommendedServerInfoScore.OK -> okServers.add(recommendedServerInfo)
                        RecommendedServerInfoScore.BAD -> Unit
                    }
                }

                if (serverFound) {
                    serverFound = false
                    return@launch
                }
                when {
                    goodServers.isNotEmpty() -> {
//                        val issuesString = createIssuesString(goodServers.first())
//                        Toast.makeText(
//                            application,
//                            issuesString,
//                            Toast.LENGTH_LONG
//                        ).show()
                        connectToServer(goodServers.first())
                    }
                    okServers.isNotEmpty() -> {
                        val okServer = okServers.first()
                        throw ExceptionUiTexts(createIssuesString(okServer))
                    }
                    else -> {
                        throw ExceptionUiText(UiText.StringResource(R.string.add_server_error_not_found))
                    }
                }
            } catch (_: CancellationException) {
            } catch (e: ExceptionUiText) {
                _uiState.emit(
                    UiState.Error(listOf(e.uiText)),
                )
            } catch (e: ExceptionUiTexts) {
                _uiState.emit(
                    UiState.Error(e.uiTexts),
                )
            } catch (e: Exception) {
                _uiState.emit(
                    UiState.Error(
                        listOf(if (e.message != null) UiText.DynamicString(e.message!!) else UiText.StringResource(R.string.unknown_error)),
                    ),
                )
            }
        }
    }

    private suspend fun connectToServer(recommendedServerInfo: RecommendedServerInfo) {
        val serverInfo = recommendedServerInfo.systemInfo.getOrNull()
            ?: throw ExceptionUiText(UiText.StringResource(R.string.add_server_error_no_id))

        Timber.d("Connecting to server: ${serverInfo.serverName}")

        val serverInDatabase = serverAlreadyInDatabase(serverInfo.id!!)

        // Check if server is already in the database
        // If so only add a new address to that server if it's different
        val server = if (serverInDatabase != null) {
            val addresses = withContext(Dispatchers.IO) {
                database.getServerWithAddresses(serverInDatabase.id).addresses
            }
            if (addresses.none { it.address == recommendedServerInfo.address }) {
                val serverAddress = ServerAddress(
                    id = UUID.randomUUID(),
                    serverId = serverInDatabase.id,
                    address = recommendedServerInfo.address,
                )

                insertServerAddress(serverAddress)
            }
            serverInDatabase
        } else {
            val serverAddress = ServerAddress(
                id = UUID.randomUUID(),
                serverId = serverInfo.id!!,
                address = recommendedServerInfo.address,
            )

            val server = Server(
                id = serverInfo.id!!,
                name = serverInfo.serverName!!,
                currentServerAddressId = serverAddress.id,
                currentUserId = null,
            )

            insertServer(server)
            insertServerAddress(serverAddress)
            server
        }

        appPreferences.currentServer = server.id

        jellyfinApi.apply {
            api.baseUrl = recommendedServerInfo.address
            api.accessToken = null
        }

        _uiState.emit(UiState.Normal)
        eventsChannel.send(AddServerEvent.NavigateToLogin)
    }

    /**
     * Create a presentable string of issues with a server
     *
     * @param server The server with issues
     * @return A presentable string of issues separated with \n
     */
    private fun createIssuesString(server: RecommendedServerInfo): Collection<UiText> {
        return server.issues.map {
            when (it) {
                is RecommendedServerIssue.OutdatedServerVersion -> {
                    UiText.StringResource(R.string.add_server_error_outdated, it.version)
                }
                is RecommendedServerIssue.InvalidProductName -> {
                    UiText.StringResource(R.string.add_server_error_not_jellyfin, it.productName)
                }
                is RecommendedServerIssue.UnsupportedServerVersion -> {
                    UiText.StringResource(R.string.add_server_error_version, it.version)
                }
                is RecommendedServerIssue.SlowResponse -> {
                    UiText.StringResource(R.string.add_server_error_slow, it.responseTime)
                }
                else -> {
                    UiText.StringResource(R.string.unknown_error)
                }
            }
        }
    }

    /**
     * Check if server is already in database using server ID
     *
     * @param id Server ID
     * @return [Server] if in database
     */
    private suspend fun serverAlreadyInDatabase(id: String) = withContext(Dispatchers.IO) {
        database.get(id)
    }

    /**
     * Add server to the database
     *
     * @param server The server
     */
    private suspend fun insertServer(server: Server) {
        withContext(Dispatchers.IO) {
            database.insertServer(server)
        }
    }

    /**
     * Add server address to the database
     *
     * @param address The address
     */
    private suspend fun insertServerAddress(address: ServerAddress) {
        withContext(Dispatchers.IO) {
            database.insertServerAddress(address)
        }
    }
}

sealed interface AddServerEvent {
    data object NavigateToLogin : AddServerEvent
}
