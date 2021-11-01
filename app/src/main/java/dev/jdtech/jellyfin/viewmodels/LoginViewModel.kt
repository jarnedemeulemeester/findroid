package dev.jdtech.jellyfin.viewmodels

import android.content.SharedPreferences
import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.database.Server
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import kotlinx.coroutines.Dispatchers
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
    private val sharedPreferences: SharedPreferences,
    private val jellyfinApi: JellyfinApi,
    private val database: ServerDatabaseDao
) : ViewModel() {

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error


    private val _navigateToMain = MutableLiveData<Boolean>()
    val navigateToMain: LiveData<Boolean> = _navigateToMain

    /**
     * Send a authentication request to the Jellyfin server
     *
     * @param username Username
     * @param password Password
     */
    fun login(username: String, password: String) {
        _error.value = null

        viewModelScope.launch {
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

                _navigateToMain.value = true
            } catch (e: Exception) {
                Timber.e(e)
                _error.value = e.message
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

    fun doneNavigatingToMain() {
        _navigateToMain.value = false
    }
}