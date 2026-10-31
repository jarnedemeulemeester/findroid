package dev.jdtech.jellyfin.setup.presentation.permission

import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import dev.jdtech.jellyfin.utils.isPermissionGranted
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

@HiltViewModel
class PermissionViewModel
@Inject
constructor(
    private val appPreferences: AppPreferences,
    @ApplicationContext private val context: Context,
    private val requests: List<PermissionRequest>,
) : ViewModel() {
    private val _state = MutableStateFlow(PermissionState())
    val state = _state.asStateFlow()

    init {
        selectCurrentRequest()

        // Re-select when a decided flag changes, including writes from outside this VM.
        requests.forEach { request ->
            appPreferences
                .observe(request.decidedPreference)
                .onEach { selectCurrentRequest() }
                .launchIn(viewModelScope)
        }
    }

    fun onAction(action: PermissionAction) {
        when (action) {
            is PermissionAction.Check -> selectCurrentRequest()
            is PermissionAction.OnSkipClick -> decideCurrent()
            is PermissionAction.OnPermissionResult -> onPermissionResult(action.granted)
            is PermissionAction.OnPermanentlyDenied ->
                _state.update { it.copy(permanentlyDenied = true) }
            is PermissionAction.OnDismiss -> _state.update { it.copy(dismissedThisSession = true) }
        }
    }

    /**
     * Selects the first request needing a prompt: one that applies on this OS version, is not
     * granted, and has not been decided. Only an explicit grant or skip persists the decided flag,
     * so implicit system grants don't suppress the prompt.
     */
    private fun selectCurrentRequest() {
        val current = requests.firstOrNull { request ->
            Build.VERSION.SDK_INT >= request.permission.minSdk &&
                !isPermissionGranted(context, request.permission) &&
                !appPreferences.getValue(request.decidedPreference)
        }

        _state.update { state ->
            if (state.currentRequest == current) {
                state
            } else {
                // Reset the denial state for the next request.
                state.copy(currentRequest = current, permanentlyDenied = false)
            }
        }
    }

    private fun onPermissionResult(granted: Boolean) {
        if (granted) {
            decideCurrent()
        }
    }

    private fun decideCurrent() {
        _state.value.currentRequest?.let { request ->
            appPreferences.setValue(request.decidedPreference, true)
        }
        selectCurrentRequest()
    }
}
