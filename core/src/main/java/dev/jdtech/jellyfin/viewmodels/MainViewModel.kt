package dev.jdtech.jellyfin.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.models.Server
import dev.jdtech.jellyfin.models.User
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel
@Inject
constructor(private val appPreferences: AppPreferences, private val database: ServerDatabaseDao) :
    ViewModel() {
    private val _state = MutableStateFlow(MainState())
    val state = _state.asStateFlow()

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    sealed class UiState {
        data class Normal(val server: Server?, val user: User?) : UiState()

        data object Loading : UiState()
    }

    init {
        check()
    }

    fun loadServerAndUser() {
        viewModelScope.launch {
            val serverId = appPreferences.getValue(appPreferences.currentServer)
            serverId?.let { id ->
                database.getServerWithAddressAndUser(id)?.let { data ->
                    _uiState.emit(UiState.Normal(data.server, data.user))
                }
            }
        }
    }

    /**
     * Continuously derives [MainState] from the underlying preferences and database, so the state
     * (and anything derived from it, e.g. the setup/main navigation condition) reacts to changes as
     * soon as they happen.
     */
    private fun check() {
        viewModelScope.launch {
            _state.emit(MainState(isLoading = true))

            val setupFlags =
                appPreferences.observe(appPreferences.currentServer).flatMapLatest { serverId ->
                    if (serverId == null) {
                        database.observeServersCount().map { serverCount ->
                            SetupFlags(
                                hasServers = serverCount > 0,
                                hasCurrentServer = false,
                                hasCurrentUser = false,
                            )
                        }
                    } else {
                        combine(
                            database.observeServersCount(),
                            database.observeServer(serverId),
                            database.observeServerCurrentUserId(serverId),
                        ) { serverCount, server, currentUserId ->
                            SetupFlags(
                                hasServers = serverCount > 0,
                                hasCurrentServer = server != null,
                                hasCurrentUser = currentUserId != null,
                            )
                        }
                    }
                }

            combine(
                    setupFlags,
                    appPreferences.observe(appPreferences.dynamicColors),
                    appPreferences.observe(appPreferences.offlineMode),
                ) { flags, dynamicColors, offlineMode ->
                    MainState(
                        isLoading = false,
                        isDynamicColors = dynamicColors,
                        hasServers = flags.hasServers,
                        hasCurrentServer = flags.hasCurrentServer,
                        hasCurrentUser = flags.hasCurrentUser,
                        isOfflineMode = offlineMode,
                    )
                }
                .collect { mainState -> _state.emit(mainState) }
        }
    }

    private data class SetupFlags(
        val hasServers: Boolean,
        val hasCurrentServer: Boolean,
        val hasCurrentUser: Boolean,
    )
}

data class MainState(
    val isLoading: Boolean = true,
    val isDynamicColors: Boolean = true,
    val hasServers: Boolean = false,
    val hasCurrentServer: Boolean = false,
    val hasCurrentUser: Boolean = false,
    val isOfflineMode: Boolean = false,
)
