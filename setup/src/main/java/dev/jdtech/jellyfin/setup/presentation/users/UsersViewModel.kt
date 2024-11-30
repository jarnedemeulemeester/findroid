package dev.jdtech.jellyfin.setup.presentation.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.setup.domain.SetupRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class UsersViewModel
@Inject
constructor(
    private val repository: SetupRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(UsersState())
    val state = _state.asStateFlow()

    private val eventsChannel = Channel<UsersEvent>()
    val events = eventsChannel.receiveAsFlow()

    fun loadUsers() {
        viewModelScope.launch {
            val server = repository.getCurrentServer() ?: return@launch
            val users = repository.getUsers(server.id)

            _state.emit(UsersState(users = users, serverName = server.name))
        }
    }

    /**
     * Delete user from database
     *
     * @param userId The id of the user
     */
    private fun deleteUser(userId: UUID) {
        viewModelScope.launch {
            repository.deleteUser(userId)
            loadUsers()
        }
    }

    private fun loginAsUser(userId: UUID) {
        viewModelScope.launch {
            repository.setCurrentUser(userId)

            eventsChannel.send(UsersEvent.NavigateToHome)
        }
    }

    fun onAction(action: UsersAction) {
        when (action) {
            is UsersAction.OnUserClick -> {
                loginAsUser(action.userId)
            }
            is UsersAction.OnDeleteUser -> {
                deleteUser(action.userId)
            }
            else -> Unit
        }
    }
}
