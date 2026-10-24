package dev.jdtech.jellyfin.setup.presentation.permission

data class PermissionState(
    val isApplicable: Boolean = false,
    val isGranted: Boolean = false,
    val isDecided: Boolean = false,
    val permanentlyDenied: Boolean = false,
    val dismissedThisSession: Boolean = false,
) {
    /** Whether the upgrader interstitial should be shown before the home screen. */
    val showInterstitial: Boolean
        get() = isApplicable && !isGranted && !isDecided && !dismissedThisSession
}
