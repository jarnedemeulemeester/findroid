package dev.jdtech.jellyfin.setup.presentation.permission

data class PermissionState(
    val currentRequest: PermissionRequest? = null,
    val permanentlyDenied: Boolean = false,
    val dismissedThisSession: Boolean = false,
) {
    /** Whether the upgrader interstitial should be shown before the home screen. */
    val showInterstitial: Boolean
        get() = currentRequest != null && !dismissedThisSession
}
