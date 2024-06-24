package dev.jdtech.jellyfin.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.AppPreferences
import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.models.Server
import dev.jdtech.jellyfin.models.User
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserSelectViewModel
@Inject
constructor(
    appPreferences: AppPreferences,
    private val jellyfinApi: JellyfinApi,
    private val database: ServerDatabaseDao,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val eventsChannel = Channel<UserSelectEvent>()
    val eventsChannelFlow = eventsChannel.receiveAsFlow()

    sealed class UiState {
        data class Normal(val server: Server, val users: List<User>) : UiState()
        data object Loading : UiState()
        data class Error(val error: Exception) : UiState()
    }

    private val currentServerId: String? = appPreferences.currentServer

    /**
     * Load users from the database and emit them
     */
    fun loadUsers() {
        viewModelScope.launch {
            _uiState.emit(UiState.Loading)
            if (currentServerId == null) {
                _uiState.emit(UiState.Error(Exception("No server in use")))
                return@launch
            }
            try {
                val serverWithUser = database.getServerWithUsers(currentServerId)
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
            if (currentServerId == null) {
                return@launch
            }
            val server = database.get(currentServerId) ?: return@launch
            server.currentUserId = user.id
            database.update(server)

            jellyfinApi.apply {
                api.update(
                    accessToken = user.accessToken,
                )
                userId = user.id
            }

            eventsChannel.send(UserSelectEvent.NavigateToMain)
        }
    }
}

sealed interface UserSelectEvent {
    data object NavigateToMain : UserSelectEvent
}
