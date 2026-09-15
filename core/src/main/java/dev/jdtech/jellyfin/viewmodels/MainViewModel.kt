package dev.jdtech.jellyfin.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.connectivity.ConnectivityMonitor
import dev.jdtech.jellyfin.connectivity.ConnectivityState
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.models.Server
import dev.jdtech.jellyfin.models.User
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel
@Inject
constructor(
    private val appPreferences: AppPreferences,
    private val database: ServerDatabaseDao,
    private val connectivityMonitor: ConnectivityMonitor,
) :
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
        observeConnectivity()
    }

    private fun observeConnectivity() {
        viewModelScope.launch {
            connectivityMonitor.state.collect { connectivityState ->
                _state.update {
                    it.copy(isOfflineMode = checkIsOfflineMode()
                        || connectivityState != ConnectivityState.Online)
                }
            }
        }
    }

    private fun check() {
        viewModelScope.launch {
            _state.emit(MainState(isLoading = true))
            val mainState =
                MainState(
                    isLoading = false,
                    isDynamicColors = checkIsDynamicColors(),
                    hasServers = checkHasServers(),
                    hasCurrentServer = checkHasCurrentServer(),
                    hasCurrentUser = checkHasCurrentUser(),
                    isOfflineMode =
                        checkIsOfflineMode() ||
                            connectivityMonitor.state.value != ConnectivityState.Online,
                )
            _state.emit(mainState)
        }
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

    private suspend fun checkHasServers(): Boolean {
        val nServers = database.getServersCount()
        return nServers > 0
    }

    private suspend fun checkHasCurrentServer(): Boolean {
        return appPreferences.getValue(appPreferences.currentServer)?.let {
            database.getServer(it) != null
        } == true
    }

    private suspend fun checkHasCurrentUser(): Boolean {
        return appPreferences.getValue(appPreferences.currentServer)?.let {
            database.getServerCurrentUser(it) != null
        } == true
    }

    private fun checkIsDynamicColors(): Boolean {
        return appPreferences.getValue(appPreferences.dynamicColors)
    }

    private fun checkIsOfflineMode(): Boolean {
        return appPreferences.getValue(appPreferences.offlineMode)
    }
}

data class MainState(
    val isLoading: Boolean = true,
    val isDynamicColors: Boolean = true,
    val hasServers: Boolean = false,
    val hasCurrentServer: Boolean = false,
    val hasCurrentUser: Boolean = false,
    val isOfflineMode: Boolean = false,
)
