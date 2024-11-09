package dev.jdtech.jellyfin.setup.presentation.addserver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.AppPreferences
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
class AddServerViewModel
@Inject
constructor(
    private val repository: SetupRepository,
    private val appPreferences: AppPreferences,
) : ViewModel() {
    private val _state = MutableStateFlow(AddServerState())
    val state = _state.asStateFlow()

    private val eventsChannel = Channel<AddServerEvent>()
    val events = eventsChannel.receiveAsFlow()

    private fun connectToServer(address: String) {
        viewModelScope.launch {
            _state.emit(
                _state.value.copy(isLoading = true, error = null),
            )

            try {
                val server = repository.connectToServer(address)
                appPreferences.currentServer = server.id
                _state.emit(
                    _state.value.copy(isLoading = false, error = null),
                )
                eventsChannel.send(AddServerEvent.Success)
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

    fun onAction(action: AddServerAction) {
        when (action) {
            is AddServerAction.OnConnectClick -> {
                connectToServer(address = action.address)
            }
            else -> Unit
        }
    }
}
