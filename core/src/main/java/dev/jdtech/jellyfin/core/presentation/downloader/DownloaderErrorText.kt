package dev.jdtech.jellyfin.core.presentation.downloader

import dev.jdtech.jellyfin.models.UiText
import dev.jdtech.jellyfin.offline.download.OfflineDownloadFailure
import dev.jdtech.jellyfin.offline.download.OfflineDownloadFailureKind

internal fun OfflineDownloadFailure.toDownloaderErrorText(): UiText =
    UiText.DynamicString(kind.defaultMessage())

private fun OfflineDownloadFailureKind.defaultMessage(): String =
    when (this) {
        OfflineDownloadFailureKind.PermissionRequired ->
            "Allow all files access to save downloads in Movies/Findroid."
        OfflineDownloadFailureKind.PathProjectionUnavailable ->
            "Could not map this Jellyfin item to a local folder."
        OfflineDownloadFailureKind.InvalidProjectedPath ->
            "The Jellyfin folder path cannot be used as a local file path."
        OfflineDownloadFailureKind.MissingRequiredAsset -> "No downloadable video source was found."
        OfflineDownloadFailureKind.CollisionWithForeignFile ->
            "A different file already exists at the target path."
        OfflineDownloadFailureKind.AuthExpired -> "Jellyfin login expired. Sign in again."
        OfflineDownloadFailureKind.Forbidden -> "Jellyfin denied access to this item."
        OfflineDownloadFailureKind.ProfileUnsupported ->
            "The server cannot create the selected offline quality."
        OfflineDownloadFailureKind.NetworkUnavailable -> "Network is unavailable. The download will retry."
        OfflineDownloadFailureKind.ServerUnavailable ->
            "Jellyfin server is unavailable. The download will retry."
        OfflineDownloadFailureKind.Server5xx -> "Jellyfin server error. The download will retry."
        OfflineDownloadFailureKind.RateLimited -> "Jellyfin is busy. The download will retry."
        OfflineDownloadFailureKind.StreamInterrupted -> "Download stream was interrupted. It will retry."
        OfflineDownloadFailureKind.StorageRootUnavailable ->
            "Movies/Findroid storage is unavailable."
        OfflineDownloadFailureKind.InsufficientSpace -> "Not enough free storage for this download."
        OfflineDownloadFailureKind.ResumeRejected ->
            "The server rejected resume. The download will restart."
        OfflineDownloadFailureKind.IntegrityFailed ->
            "Downloaded video did not validate. The download will retry."
        OfflineDownloadFailureKind.AppInterrupted ->
            "Download was interrupted by app shutdown. It will recover."
        OfflineDownloadFailureKind.SourceMissingOrChanged ->
            "The Jellyfin source is missing or changed. Refresh the item and try again."
        OfflineDownloadFailureKind.PublishFailed ->
            "Could not publish the file into Movies/Findroid."
        OfflineDownloadFailureKind.ScanFailed ->
            "The file downloaded, but Android media scan failed."
        OfflineDownloadFailureKind.Canceled -> "Download canceled."
    }
