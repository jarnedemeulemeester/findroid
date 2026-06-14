package dev.jdtech.jellyfin.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.jdtech.jellyfin.offline.download.OfflineAsset
import dev.jdtech.jellyfin.offline.download.OfflineAssetKind
import dev.jdtech.jellyfin.offline.download.OfflineAssetRequiredness
import dev.jdtech.jellyfin.offline.download.OfflineAssetStatus
import dev.jdtech.jellyfin.offline.download.OfflineDownloadFailure
import dev.jdtech.jellyfin.offline.download.OfflineDownloadFailureKind
import dev.jdtech.jellyfin.offline.download.OfflineStorageScope

@Entity(
    tableName = "offlineAssets",
    foreignKeys =
        [
            ForeignKey(
                entity = OfflinePackageDto::class,
                parentColumns = ["packageId"],
                childColumns = ["packageId"],
                onDelete = ForeignKey.CASCADE,
            )
        ],
    indices = [Index("packageId"), Index("ownerItemId"), Index("kind"), Index("status")],
)
data class OfflineAssetDto(
    @PrimaryKey val assetId: String,
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
    val status: OfflineAssetStatus,
    val failureKind: OfflineDownloadFailureKind? = null,
    val failureMessage: String? = null,
    val retryCount: Int = 0,
    val updatedAtMillis: Long,
)

fun OfflineAsset.toOfflineAssetDto(updatedAtMillis: Long): OfflineAssetDto =
    OfflineAssetDto(
        assetId = assetId,
        packageId = packageId,
        kind = kind,
        ownerItemId = ownerItemId,
        sourceId = sourceId,
        profileId = profileId,
        imageType = imageType,
        imageIndex = imageIndex,
        imageTag = imageTag,
        blurHash = blurHash,
        width = width,
        height = height,
        mimeType = mimeType,
        storageScope = storageScope,
        tempPath = tempPath,
        finalPath = finalPath,
        bytes = bytes,
        requiredness = requiredness,
        status = status,
        failureKind = failure?.kind,
        failureMessage = failure?.message,
        retryCount = retryCount,
        updatedAtMillis = updatedAtMillis,
    )

fun OfflineAssetDto.toOfflineAsset(): OfflineAsset =
    OfflineAsset(
        assetId = assetId,
        packageId = packageId,
        kind = kind,
        ownerItemId = ownerItemId,
        sourceId = sourceId,
        profileId = profileId,
        imageType = imageType,
        imageIndex = imageIndex,
        imageTag = imageTag,
        blurHash = blurHash,
        width = width,
        height = height,
        mimeType = mimeType,
        storageScope = storageScope,
        tempPath = tempPath,
        finalPath = finalPath,
        bytes = bytes,
        requiredness = requiredness,
        status = status,
        failure = failureKind?.let { OfflineDownloadFailure(it, failureMessage) },
        retryCount = retryCount,
    )
