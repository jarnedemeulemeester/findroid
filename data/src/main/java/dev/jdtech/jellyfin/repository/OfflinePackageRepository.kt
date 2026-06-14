package dev.jdtech.jellyfin.repository

import dev.jdtech.jellyfin.offline.download.OfflineAsset
import dev.jdtech.jellyfin.offline.download.OfflineAssetStatus
import dev.jdtech.jellyfin.offline.download.OfflineDownloadFailure
import dev.jdtech.jellyfin.offline.download.OfflineItemSnapshot
import dev.jdtech.jellyfin.offline.download.OfflinePackageManifest
import dev.jdtech.jellyfin.offline.download.OfflinePackageReadiness
import kotlinx.coroutines.flow.Flow

interface OfflinePackageRepository {
    suspend fun savePackage(
        serverId: String,
        manifest: OfflinePackageManifest,
        itemSnapshot: OfflineItemSnapshot?,
        nowMillis: Long,
    )

    suspend fun getPackage(packageId: String): OfflinePackageManifest?

    fun observePackage(packageId: String): Flow<OfflinePackageManifest?>

    suspend fun getPackagesByItemId(itemId: String): List<OfflinePackageManifest>

    suspend fun getPackagesByServerId(serverId: String): List<OfflinePackageManifest>

    suspend fun getReadyItemSnapshotsByServerId(serverId: String): List<OfflineItemSnapshot>

    suspend fun getAllPackages(): List<OfflinePackageManifest>

    suspend fun getInterruptedActivePublicVideoPackageIds(): List<String>

    suspend fun getCanceledPublicVideoPackageIdsWithTempPath(): List<String>

    suspend fun setPackageReadiness(
        packageId: String,
        readiness: OfflinePackageReadiness,
        nowMillis: Long,
    )

    suspend fun setAssetState(
        asset: OfflineAsset,
        status: OfflineAssetStatus,
        failure: OfflineDownloadFailure?,
        bytes: Long?,
        tempPath: String?,
        finalPath: String?,
        retryCount: Int,
        nowMillis: Long,
    )

    suspend fun markInterruptedActivePublicVideoAssets(nowMillis: Long): Int

    suspend fun clearCanceledPublicVideoTempPaths(nowMillis: Long): Int

    suspend fun deletePackage(packageId: String)
}
