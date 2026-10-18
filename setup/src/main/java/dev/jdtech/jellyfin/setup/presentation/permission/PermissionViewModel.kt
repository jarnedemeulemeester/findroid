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
    val request: PermissionRequest,
) : ViewModel() {
    private val _state = MutableStateFlow(PermissionState())
    val state = _state.asStateFlow()

    init {
        onAction(PermissionAction.Check)

        // The decided flag is the single source of truth connecting the instances of this
        // ViewModel used by the interstitial gate and the permission screen.
        appPreferences
            .observe(request.decidedPreference)
            .onEach { decided -> _state.update { it.copy(isDecided = decided) } }
            .launchIn(viewModelScope)
    }

    fun onAction(action: PermissionAction) {
        when (action) {
            is PermissionAction.Check -> check()
            is PermissionAction.OnSkipClick -> decide()
            is PermissionAction.OnPermissionResult -> onPermissionResult(action.granted)
            is PermissionAction.OnPermanentlyDenied ->
                _state.update { it.copy(permanentlyDenied = true) }
            is PermissionAction.OnDismiss -> _state.update { it.copy(dismissedThisSession = true) }
        }
    }

    /**
     * Re-reads the permission state. No-op below [PermissionRequest.applicableSince]: the
     * permission is unknown to the OS there and the decided flag must not be set, so that users
     * whose device later upgrades still get the prompt once.
     */
    private fun check() {
        val isApplicable = Build.VERSION.SDK_INT >= request.applicableSince
        val isGranted = isPermissionGranted(context, request.permission)
        var isDecided = appPreferences.getValue(request.decidedPreference)

        // If the permission is already granted, e.g. carried over from the NEARBY_DEVICES
        // permission group, mark it as decided without ever showing the screen.
        if (isApplicable && isGranted && !isDecided) {
            appPreferences.setValue(request.decidedPreference, true)
            isDecided = true
        }

        _state.update {
            it.copy(isApplicable = isApplicable, isGranted = isGranted, isDecided = isDecided)
        }
    }

    private fun onPermissionResult(granted: Boolean) {
        _state.update { it.copy(isGranted = granted, permanentlyDenied = false) }
        if (granted) {
            decide()
        }
    }

    private fun decide() {
        if (!appPreferences.getValue(request.decidedPreference)) {
            appPreferences.setValue(request.decidedPreference, true)
        }
        _state.update { it.copy(isDecided = true) }
    }
}
