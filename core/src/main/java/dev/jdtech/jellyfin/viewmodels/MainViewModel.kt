package dev.jdtech.jellyfin.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.AppPreferences
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.models.Server
import dev.jdtech.jellyfin.models.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel
@Inject
constructor(
    private val appPreferences: AppPreferences,
    private val database: ServerDatabaseDao,
) : ViewModel() {
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

    private fun check() {
        viewModelScope.launch {
            _state.emit(MainState(isLoading = true))
            val mainState = MainState(
                isLoading = false,
                hasServers = checkHasServers(),
                hasCurrentServer = checkHasCurrentServer(),
                hasCurrentUser = checkHasCurrentUser(),
            )
            _state.emit(mainState)
        }
    }

    fun loadServerAndUser() {
        viewModelScope.launch {
            val serverId = appPreferences.currentServer
            serverId?.let { id ->
                database.getServerWithAddressAndUser(id)?.let { data ->
                    _uiState.emit(
                        UiState.Normal(data.server, data.user),
                    )
                }
            }
        }
    }

    private fun checkHasServers(): Boolean {
        val nServers = database.getServersCount()
        return nServers > 0
    }

    private fun checkHasCurrentServer(): Boolean {
        return appPreferences.currentServer?.let {
            database.get(it) != null
        } == true
    }

    private fun checkHasCurrentUser(): Boolean {
        return appPreferences.currentServer?.let {
            database.getServerCurrentUser(it) != null
        } == true
    }
}

data class MainState(
    val isLoading: Boolean = true,
    val hasServers: Boolean = false,
    val hasCurrentServer: Boolean = false,
    val hasCurrentUser: Boolean = false,
)
