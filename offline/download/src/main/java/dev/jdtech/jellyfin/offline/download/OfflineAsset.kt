package dev.jdtech.jellyfin.offline.download

enum class OfflineAssetKind {
    VIDEO,
    SUBTITLE,
    POSTER_PRIMARY,
    BACKDROP,
    LOGO,
    SERIES_PRIMARY,
    SERIES_BACKDROP,
    SERIES_LOGO,
    SEASON_PRIMARY,
    CHAPTER_IMAGE,
    TRICKPLAY_TILE,
    PUBLIC_FOLDER_POSTER,
}

enum class OfflineAssetRequiredness {
    PLAYBACK_REQUIRED,
    PACKAGE_REQUIRED,
    OPTIONAL,
}

enum class OfflineAssetStatus {
    PLANNED,
    QUEUED,
    DOWNLOADING,
    VERIFYING,
    READY,
    RETRY_WAIT,
    FAILED_OPTIONAL,
    FAILED_REQUIRED,
    SKIPPED_NOT_AVAILABLE,
}

enum class OfflineStorageScope {
    PUBLIC_MEDIA,
    APP_PRIVATE,
    HIDDEN_WORK,
}

data class OfflineAsset(
    val assetId: String,
    val packageId: String,
    val kind: OfflineAssetKind,
    val ownerItemId: String,
    val sourceId: String? = null,
    val profileId: String? = null,
    val imageType: String? = null,
    val imageIndex: Int? = null,
    val imageTag: String? = null,
    val blurHash: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val mimeType: String? = null,
    val storageScope: OfflineStorageScope,
    val tempPath: String? = null,
    val finalPath: String? = null,
    val bytes: Long? = null,
    val requiredness: OfflineAssetRequiredness,
    val status: OfflineAssetStatus = OfflineAssetStatus.PLANNED,
    val failure: OfflineDownloadFailure? = null,
    val retryCount: Int = 0,
) {
    val isReady: Boolean
        get() = status == OfflineAssetStatus.READY

    val isSatisfied: Boolean
        get() =
            status == OfflineAssetStatus.READY ||
                (requiredness == OfflineAssetRequiredness.OPTIONAL &&
                    status == OfflineAssetStatus.SKIPPED_NOT_AVAILABLE)

    val isFailed: Boolean
        get() =
            status == OfflineAssetStatus.FAILED_OPTIONAL ||
                status == OfflineAssetStatus.FAILED_REQUIRED
}
