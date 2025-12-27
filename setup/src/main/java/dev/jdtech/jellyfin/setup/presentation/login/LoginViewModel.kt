package dev.jdtech.jellyfin.setup.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.models.UiText
import dev.jdtech.jellyfin.setup.R as SetupR
import dev.jdtech.jellyfin.setup.domain.SetupRepository
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@HiltViewModel
class LoginViewModel @Inject constructor(private val repository: SetupRepository) : ViewModel() {
    private val _state = MutableStateFlow(LoginState())
    val state = _state.asStateFlow()

    private val eventsChannel = Channel<LoginEvent>()
    val events = eventsChannel.receiveAsFlow()

    private var quickConnectJob: Job? = null

    fun loadServer() {
        viewModelScope.launch {
            try {
                val server = repository.getCurrentServer()
                _state.emit(_state.value.copy(serverName = server?.name))
            } catch (_: Exception) {}
        }
    }

    fun loadDisclaimer() {
        viewModelScope.launch {
            try {
                val loginDisclaimer = repository.loadDisclaimer()
                _state.emit(_state.value.copy(disclaimer = loginDisclaimer))
            } catch (_: Exception) {}
        }
    }

    fun loadQuickConnectEnabled() {
        viewModelScope.launch {
            try {
                val isEnabled = repository.getIsQuickConnectEnabled()
                _state.emit(_state.value.copy(quickConnectEnabled = isEnabled))
            } catch (_: Exception) {}
        }
    }

    private fun login(username: String, password: String) {
        viewModelScope.launch {
            try {
                _state.emit(_state.value.copy(isLoading = true, error = null))
                repository.login(username, password)
                _state.emit(_state.value.copy(isLoading = false))
                eventsChannel.send(LoginEvent.Success)
            } catch (e: Exception) {
                val message =
                    if (e.message?.contains("401") == true) {
                        UiText.StringResource(SetupR.string.login_error_wrong_username_password)
                    } else {
                        UiText.StringResource(CoreR.string.unknown_error)
                    }
                _state.emit(_state.value.copy(isLoading = false, error = message))
            }
        }
    }

    private fun quickConnect() {
        if (quickConnectJob?.isActive == true) {
            quickConnectJob?.cancel()
            return
        }
        quickConnectJob =
            viewModelScope.launch {
                try {
                    var quickConnectState = repository.initiateQuickConnect()
                    _state.emit(_state.value.copy(quickConnectCode = quickConnectState.code))

                    while (!quickConnectState.authenticated) {
                        delay(5000L)
                        quickConnectState =
                            repository.getQuickConnectState(quickConnectState.secret)
                    }

                    repository.loginWithSecret(quickConnectState.secret)

                    _state.emit(_state.value.copy(quickConnectCode = null))
                    eventsChannel.send(LoginEvent.Success)
                } catch (_: Exception) {
                    _state.emit(_state.value.copy(quickConnectCode = null))
                }
            }
    }

    fun onAction(action: LoginAction) {
        when (action) {
            is LoginAction.OnLoginClick -> {
                login(action.username, action.password)
            }
            is LoginAction.OnQuickConnectClick -> {
                quickConnect()
            }
            else -> Unit
        }
    }
}
