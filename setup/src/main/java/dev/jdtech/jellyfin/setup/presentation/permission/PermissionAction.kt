package dev.jdtech.jellyfin.setup.presentation.permission

sealed interface PermissionAction {
    /** Re-reads the current permission state, e.g. when returning from the system settings. */
    data object Check : PermissionAction

    /** The user explicitly skipped the permission. Records the decision. */
    data object OnSkipClick : PermissionAction

    data class OnPermissionResult(val granted: Boolean) : PermissionAction

    /** The system permission dialog was suppressed ("don't ask again"). */
    data object OnPermanentlyDenied : PermissionAction

    /** Dismisses the interstitial for the current session without recording a decision. */
    data object OnDismiss : PermissionAction
}
