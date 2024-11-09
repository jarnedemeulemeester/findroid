package dev.jdtech.jellyfin.setup.presentation.discoverserver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.AppPreferences
import dev.jdtech.jellyfin.models.DiscoveredServer
import dev.jdtech.jellyfin.models.ExceptionUiText
import dev.jdtech.jellyfin.models.ExceptionUiTexts
import dev.jdtech.jellyfin.models.UiText
import dev.jdtech.jellyfin.setup.domain.SetupRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import dev.jdtech.jellyfin.core.R as CoreR

@HiltViewModel
class DiscoverServerViewModel
@Inject
constructor(
    private val repository: SetupRepository,
    private val appPreferences: AppPreferences,
) : ViewModel() {
    private val _state = MutableStateFlow(DiscoverServerState())
    val state = _state.asStateFlow()

    private val eventsChannel = Channel<DiscoverServerEvent>()
    val events = eventsChannel.receiveAsFlow()

    private val discoveredServers = mutableListOf<DiscoveredServer>()

    fun discoverServers() {
        viewModelScope.launch {
            _state.emit(
                _state.value.copy(isLoading = true, error = null),
            )
            discoveredServers.clear()
            val serversDiscovery = repository.discoverServers()
            serversDiscovery.collect { serverDiscoveryInfo ->
                discoveredServers.add(
                    DiscoveredServer(
                        serverDiscoveryInfo.id,
                        serverDiscoveryInfo.name,
                        serverDiscoveryInfo.address,
                    ),
                )
                _state.emit(
                    _state.value.copy(servers = discoveredServers),
                )
            }

            _state.emit(
                _state.value.copy(isLoading = false),
            )
        }
    }

    private fun connectToServer(address: String) {
        viewModelScope.launch {
            _state.emit(
                _state.value.copy(isLoading = true),
            )

            try {
                val server = repository.connectToServer(address)
                appPreferences.currentServer = server.id
                _state.emit(
                    _state.value.copy(isLoading = false, error = null),
                )
                eventsChannel.send(DiscoverServerEvent.Success)
            } catch (_: CancellationException) {
            } catch (e: ExceptionUiText) {
                _state.emit(
                    _state.value.copy(isLoading = false, error = listOf(e.uiText)),
                )
            } catch (e: ExceptionUiTexts) {
                _state.emit(
                    _state.value.copy(isLoading = false, error = e.uiTexts),
                )
            } catch (e: Exception) {
                _state.emit(
                    _state.value.copy(
                        isLoading = false,
                        error = listOf(if (e.message != null) UiText.DynamicString(e.message!!) else UiText.StringResource(CoreR.string.unknown_error)),
                    ),
                )
            }
        }
    }

    fun onAction(action: DiscoverServerAction) {
        when (action) {
            is DiscoverServerAction.OnServerClick -> {
                connectToServer(address = action.address)
            }
            else -> Unit
        }
    }
}
