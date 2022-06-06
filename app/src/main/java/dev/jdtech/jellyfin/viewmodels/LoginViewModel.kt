package dev.jdtech.jellyfin.viewmodels

import android.content.SharedPreferences
import android.content.res.Resources
import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.BaseApplication
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.database.Server
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.model.api.AuthenticateUserByName
import timber.log.Timber
import java.lang.Exception
import javax.inject.Inject

@HiltViewModel
class LoginViewModel
@Inject
constructor(
    application: BaseApplication,
    private val sharedPreferences: SharedPreferences,
    private val jellyfinApi: JellyfinApi,
    private val database: ServerDatabaseDao
) : ViewModel() {
    private val resources: Resources = application.resources

    private val uiState = MutableStateFlow<UiState>(UiState.Normal)

    private val navigateToMain = MutableSharedFlow<Boolean>()

    sealed class UiState {
        object Normal : UiState()
        object Loading : UiState()
        data class Error(val message: String) : UiState()
    }

    fun onUiState(scope: LifecycleCoroutineScope, collector: (UiState) -> Unit) {
        scope.launch { uiState.collect { collector(it) } }
    }

    fun onNavigateToMain(scope: LifecycleCoroutineScope, collector: (Boolean) -> Unit) {
        scope.launch { navigateToMain.collect { collector(it) } }
    }

    /**
     * Send a authentication request to the Jellyfin server
     *
     * @param username Username
     * @param password Password
     */
    fun login(username: String, password: String) {
        viewModelScope.launch {
            uiState.emit(UiState.Loading)

            try {
                val authenticationResult by jellyfinApi.userApi.authenticateUserByName(
                    data = AuthenticateUserByName(
                        username = username,
                        pw = password
                    )
                )

                val serverInfo by jellyfinApi.systemApi.getPublicSystemInfo()

                val server = Server(
                    serverInfo.id!!,
                    serverInfo.serverName!!,
                    jellyfinApi.api.baseUrl!!,
                    authenticationResult.user?.id.toString(),
                    authenticationResult.user?.name!!,
                    authenticationResult.accessToken!!
                )

                insert(server)

                val spEdit = sharedPreferences.edit()
                spEdit.putString("selectedServer", server.id)
                spEdit.apply()

                jellyfinApi.apply {
                    api.accessToken = authenticationResult.accessToken
                    userId = authenticationResult.user?.id
                }

                uiState.emit(UiState.Normal)
                navigateToMain.emit(true)
            } catch (e: Exception) {
                Timber.e(e)
                val message =
                    if (e.cause?.message?.contains("401") == true) resources.getString(R.string.login_error_wrong_username_password) else resources.getString(
                        R.string.unknown_error
                    )
                uiState.emit(UiState.Error(message))
            }
        }
    }

    /**
     * Add server to the database
     *
     * @param server The server
     */
    private suspend fun insert(server: Server) {
        withContext(Dispatchers.IO) {
            database.insert(server)
        }
    }
}