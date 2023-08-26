package dev.jdtech.jellyfin.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.models.ServerAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ServerAddressesViewModel
@Inject
constructor(
    private val jellyfinApi: JellyfinApi,
    private val database: ServerDatabaseDao,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    sealed class UiState {
        data class Normal(val addresses: List<ServerAddress>) : UiState()
        data object Loading : UiState()
        data class Error(val error: Exception) : UiState()
    }

    private val _navigateToMain = MutableSharedFlow<Boolean>()
    val navigateToMain = _navigateToMain.asSharedFlow()

    private var currentServerId: String = ""

    fun loadAddresses(serverId: String) {
        currentServerId = serverId
        viewModelScope.launch {
            _uiState.emit(UiState.Loading)
            try {
                val serverWithUser = database.getServerWithAddresses(serverId)
                _uiState.emit(UiState.Normal(serverWithUser.addresses))
            } catch (e: Exception) {
                _uiState.emit(UiState.Error(e))
            }
        }
    }

    /**
     * Delete server address from database
     *
     * @param address The server address
     */
    fun deleteAddress(address: ServerAddress) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentAddress = database.getServerCurrentAddress(currentServerId)
            if (address == currentAddress) {
                Timber.e("You cannot delete the current address")
                return@launch
            }
            database.deleteServerAddress(address.id)
            loadAddresses(currentServerId)
        }
    }

    fun switchToAddress(address: ServerAddress) {
        viewModelScope.launch {
            val server = database.get(currentServerId) ?: return@launch
            server.currentServerAddressId = address.id
            database.update(server)

            jellyfinApi.api.baseUrl = address.address

            _navigateToMain.emit(true)
        }
    }

    fun addAddress(address: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val serverAddress = ServerAddress(UUID.randomUUID(), currentServerId, address)
            database.insertServerAddress(serverAddress)
            loadAddresses(currentServerId)
        }
    }
}
