package dev.jdtech.jellyfin.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.models.Server
import dev.jdtech.jellyfin.models.User
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserSelectViewModel
@Inject
constructor(
    private val jellyfinApi: JellyfinApi,
    private val database: ServerDatabaseDao,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _navigateToMain = MutableSharedFlow<Boolean>()
    val navigateToMain = _navigateToMain.asSharedFlow()

    sealed class UiState {
        data class Normal(val server: Server, val users: List<User>) : UiState()
        object Loading : UiState()
        data class Error(val error: Exception) : UiState()
    }

    private var currentServerId: String = ""

    /**
     * Load users from the database and emit them
     *
     * @param serverId The ID of the server
     */
    fun loadUsers(serverId: String) {
        currentServerId = serverId
        viewModelScope.launch {
            _uiState.emit(UiState.Loading)
            try {
                val serverWithUser = database.getServerWithUsers(serverId)
                _uiState.emit(UiState.Normal(serverWithUser.server, serverWithUser.users))
            } catch (e: Exception) {
                _uiState.emit(UiState.Error(e))
            }
        }
    }

    /**
     * Log in as user and navigate to home screen
     *
     * @param user The user
     */
    fun loginAsUser(user: User) {
        viewModelScope.launch {
            val server = database.get(currentServerId) ?: return@launch
            server.currentUserId = user.id
            database.update(server)

            jellyfinApi.apply {
                api.accessToken = user.accessToken
                userId = user.id
            }

            _navigateToMain.emit(true)
        }
    }
}
