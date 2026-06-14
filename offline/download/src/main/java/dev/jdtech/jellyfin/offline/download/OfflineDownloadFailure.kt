package dev.jdtech.jellyfin.offline.download

enum class OfflineDownloadFailureKind {
    PermissionRequired,
    PathProjectionUnavailable,
    InvalidProjectedPath,
    MissingRequiredAsset,
    CollisionWithForeignFile,
    AuthExpired,
    Forbidden,
    ProfileUnsupported,
    NetworkUnavailable,
    ServerUnavailable,
    Server5xx,
    RateLimited,
    StreamInterrupted,
    StorageRootUnavailable,
    InsufficientSpace,
    ResumeRejected,
    IntegrityFailed,
    AppInterrupted,
    SourceMissingOrChanged,
    PublishFailed,
    ScanFailed,
    Canceled,
}

enum class OfflineDownloadFailureDisposition {
    Retryable,
    UserAction,
    Terminal,
}

data class OfflineDownloadFailure(
    val kind: OfflineDownloadFailureKind,
    val message: String? = null,
) {
    val disposition: OfflineDownloadFailureDisposition
        get() =
            when (kind) {
                OfflineDownloadFailureKind.NetworkUnavailable,
                OfflineDownloadFailureKind.ServerUnavailable,
                OfflineDownloadFailureKind.Server5xx,
                OfflineDownloadFailureKind.RateLimited,
                OfflineDownloadFailureKind.StreamInterrupted,
                OfflineDownloadFailureKind.StorageRootUnavailable,
                OfflineDownloadFailureKind.InsufficientSpace,
                OfflineDownloadFailureKind.ResumeRejected,
                OfflineDownloadFailureKind.IntegrityFailed,
                OfflineDownloadFailureKind.AppInterrupted,
                OfflineDownloadFailureKind.PublishFailed,
                OfflineDownloadFailureKind.ScanFailed -> OfflineDownloadFailureDisposition.Retryable
                OfflineDownloadFailureKind.PermissionRequired,
                OfflineDownloadFailureKind.CollisionWithForeignFile,
                OfflineDownloadFailureKind.AuthExpired,
                OfflineDownloadFailureKind.Forbidden -> OfflineDownloadFailureDisposition.UserAction
                OfflineDownloadFailureKind.PathProjectionUnavailable,
                OfflineDownloadFailureKind.InvalidProjectedPath,
                OfflineDownloadFailureKind.MissingRequiredAsset,
                OfflineDownloadFailureKind.SourceMissingOrChanged,
                OfflineDownloadFailureKind.ProfileUnsupported,
                OfflineDownloadFailureKind.Canceled -> OfflineDownloadFailureDisposition.Terminal
            }
}
