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
        onAction(PermissionAction.Check)

        // The decided flags connect the interstitial gate and the permission screen.
        requests.forEach { request ->
            appPreferences
                .observe(request.decidedPreference)
                .onEach { check() }
                .launchIn(viewModelScope)
        }
    }

    fun onAction(action: PermissionAction) {
        when (action) {
            is PermissionAction.Check -> check()
            is PermissionAction.OnSkipClick -> decideCurrent()
            is PermissionAction.OnPermissionResult -> onPermissionResult(action.granted)
            is PermissionAction.OnPermanentlyDenied ->
                _state.update { it.copy(permanentlyDenied = true) }
            is PermissionAction.OnDismiss -> _state.update { it.copy(dismissedThisSession = true) }
        }
    }

    /**
     * Re-evaluates all requests and selects the first one needing a prompt. Requests below their
     * minSdk are skipped and their decided flags never set, so devices upgrading later still get
     * the prompt once.
     */
    private fun check() {
        for ((permission, decidedPreference) in requests) {
            val isApplicable = Build.VERSION.SDK_INT >= permission.minSdk
            val isGranted = isPermissionGranted(context, permission)

            // Already granted, e.g. carried over from the NEARBY_DEVICES group: mark decided
            // without prompting.
            if (isApplicable && isGranted && !appPreferences.getValue(decidedPreference)) {
                appPreferences.setValue(decidedPreference, true)
            }
        }
        selectCurrentRequest()
    }

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
                // Reset the denial state when moving on to another request.
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
        check()
    }
}
