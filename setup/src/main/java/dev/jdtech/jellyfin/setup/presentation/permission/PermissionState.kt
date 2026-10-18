package dev.jdtech.jellyfin.setup.presentation.permission

data class PermissionState(
    val isApplicable: Boolean = false,
    val isGranted: Boolean = false,
    val isDecided: Boolean = false,
    val permanentlyDenied: Boolean = false,
    val dismissedThisSession: Boolean = false,
) {
    /**
     * Whether the upgrader interstitial should be shown before the user reaches the home screen.
     * Only true on devices where the permission applies, while it is neither granted nor decided,
     * and while it was not dismissed with a back-press in the current session.
     */
    val showInterstitial: Boolean
        get() = isApplicable && !isGranted && !isDecided && !dismissedThisSession
}
