package dev.jdtech.jellyfin.core.presentation.downloader

internal class PendingPermissionAction<T> {
    private var action: T? = null

    fun remember(action: T) {
        this.action = action
    }

    fun consume(hasPermission: Boolean): PendingPermissionResult<T> {
        val pendingAction = action ?: return PendingPermissionResult.None
        action = null
        return if (hasPermission) {
            PendingPermissionResult.Resume(pendingAction)
        } else {
            PendingPermissionResult.Denied
        }
    }
}

internal sealed interface PendingPermissionResult<out T> {
    data object None : PendingPermissionResult<Nothing>

    data object Denied : PendingPermissionResult<Nothing>

    data class Resume<T>(val action: T) : PendingPermissionResult<T>
}
