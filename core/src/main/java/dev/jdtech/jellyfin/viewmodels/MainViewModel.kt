package dev.jdtech.jellyfin.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.AppPreferences
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.models.Server
import dev.jdtech.jellyfin.models.ServerAddress
import dev.jdtech.jellyfin.models.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.net.InetAddress
import java.net.UnknownHostException
import javax.inject.Inject

@HiltViewModel
class MainViewModel
@Inject
constructor(
    private val appPreferences: AppPreferences,
    private val database: ServerDatabaseDao,
) : ViewModel() {
    var startDestinationChanged = false

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    private var _addressChangedOnBoot = MutableLiveData<Boolean>()
    val addressChangedOnBoot: LiveData<Boolean> = _addressChangedOnBoot

    sealed class UiState {
        data class Normal(val server: Server?, val user: User?) : UiState()
        data object Loading : UiState()
    }

    init {
        loadServerAndUser()
    }

    private fun loadServerAndUser() {
        viewModelScope.launch {
            val serverId = appPreferences.currentServer
            serverId?.let { id ->
                database.getServerWithAddressAndUser(id)?.let { data ->
                    database.getServerWithAddresses(id).let { dataAddresses ->
                        val bestAddressDeferred = dataAddresses.addresses.map { addressData ->
                            async(Dispatchers.IO) {
                                try {
                                    Timber.e("checking ${addressData.address}")
                                    val hostname = addressData.address.removePrefix("https://").removePrefix("http://").split(':')[0].split('/')[0]
                                    if (InetAddress.getByName(hostname).isReachable(1000)){
                                        addressData
                                    }
                                    else {
                                        null
                                    }
                                } catch (e: UnknownHostException) {
                                    null
                                }
                            }
                        }

                        var bestAddress: ServerAddress? = null

                        runBlocking {
                            for (address in bestAddressDeferred) {
                                val result = address.await()
                                if (result != null) {
                                    bestAddress = result
                                }
                            }
                        }

                        val server = database.get(serverId)
                        if (bestAddress != null) {
                            Timber.i("${bestAddress!!.address} is the best address")
                            if (server?.currentServerAddressId != bestAddress!!.id) {
                                _addressChangedOnBoot.postValue(true)
                                server?.currentServerAddressId = bestAddress!!.id
                                database.update(server!!)
                            }
                        } else {
                            Timber.i("No viable addresses found")
                        }
                    }

                    _uiState.emit(
                        UiState.Normal(data.server, data.user),
                    )
                }
            }
        }
    }
}
