package dev.jdtech.jellyfin.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.models.UiText
import dev.jdtech.jellyfin.models.User
import dev.jdtech.jellyfin.utils.AppPreferences
import javax.inject.Inject
import kotlin.Exception
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.model.api.AuthenticateUserByName

@HiltViewModel
class LoginViewModel
@Inject
constructor(
    private val appPreferences: AppPreferences,
    private val jellyfinApi: JellyfinApi,
    private val database: ServerDatabaseDao
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Normal)
    val uiState = _uiState.asStateFlow()
    private val _usersState = MutableStateFlow<UsersState>(UsersState.Loading)
    val usersState = _usersState.asStateFlow()
    private val _navigateToMain = MutableSharedFlow<Boolean>()
    val navigateToMain = _navigateToMain.asSharedFlow()

    sealed class UiState {
        object Normal : UiState()
        object Loading : UiState()
        data class Error(val message: UiText) : UiState()
    }

    sealed class UsersState {
        object Loading : UsersState()
        data class Users(val users: List<User>) : UsersState()
    }

    init {
        loadPublicUsers()
    }

    private fun loadPublicUsers() {
        viewModelScope.launch {
            _usersState.emit(UsersState.Loading)
            try {
                // Local users
                val localUsers = appPreferences.currentServer?.let {
                    database.getServerWithUsers(it).users
                } ?: emptyList()

                // Public users
                val publicUsersResponse by jellyfinApi.userApi.getPublicUsers()
                val publicUsers =
                    publicUsersResponse.map { User(id = it.id, name = it.name.orEmpty(), serverId = it.serverId!!) }

                // Combine both local and public users
                val users = (localUsers + publicUsers).distinctBy { it.id }

                _usersState.emit(UsersState.Users(users))
            } catch (e: Exception) {
                _usersState.emit(UsersState.Users(emptyList()))
            }
        }
    }

    /**
     * Send a authentication request to the Jellyfin server
     *
     * @param username Username
     * @param password Password
     */
    fun login(username: String, password: String) {
        viewModelScope.launch {
            _uiState.emit(UiState.Loading)

            try {
                val authenticationResult by jellyfinApi.userApi.authenticateUserByName(
                    data = AuthenticateUserByName(
                        username = username,
                        pw = password
                    )
                )

                val serverInfo by jellyfinApi.systemApi.getPublicSystemInfo()

                val user = User(
                    id = authenticationResult.user!!.id,
                    name = authenticationResult.user!!.name!!,
                    serverId = serverInfo.id!!,
                    accessToken = authenticationResult.accessToken!!
                )

                insertUser(appPreferences.currentServer!!, user)

                jellyfinApi.apply {
                    api.accessToken = authenticationResult.accessToken
                    userId = authenticationResult.user?.id
                }

                _uiState.emit(UiState.Normal)
                _navigateToMain.emit(true)
            } catch (e: Exception) {
                val message =
                    if (e.message?.contains("401") == true) UiText.StringResource(R.string.login_error_wrong_username_password) else UiText.StringResource(
                        R.string.unknown_error
                    )
                _uiState.emit(UiState.Error(message))
            }
        }
    }

    private suspend fun insertUser(serverId: String, user: User) {
        withContext(Dispatchers.IO) {
            database.insertUser(user)
            database.updateServerCurrentUser(serverId, user.id)
        }
    }
}
