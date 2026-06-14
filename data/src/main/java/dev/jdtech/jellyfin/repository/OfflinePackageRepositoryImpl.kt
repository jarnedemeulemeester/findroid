package dev.jdtech.jellyfin.repository

import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.models.toOfflineAssetDto
import dev.jdtech.jellyfin.models.toOfflineItemSnapshotDto
import dev.jdtech.jellyfin.models.toOfflineItemSnapshot
import dev.jdtech.jellyfin.models.toOfflinePackageDto
import dev.jdtech.jellyfin.models.toOfflinePackageManifest
import dev.jdtech.jellyfin.offline.download.OfflineAsset
import dev.jdtech.jellyfin.offline.download.OfflineAssetStatus
import dev.jdtech.jellyfin.offline.download.OfflineDownloadFailure
import dev.jdtech.jellyfin.offline.download.OfflineItemSnapshot
import dev.jdtech.jellyfin.offline.download.OfflinePackageManifest
import dev.jdtech.jellyfin.offline.download.OfflinePackageReadiness
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

class OfflinePackageRepositoryImpl(
    private val database: ServerDatabaseDao,
) : OfflinePackageRepository {
    override suspend fun savePackage(
        serverId: String,
        manifest: OfflinePackageManifest,
        itemSnapshot: OfflineItemSnapshot?,
        nowMillis: Long,
    ) =
        withContext(Dispatchers.IO) {
            database.insertOfflinePackageWithAssets(
                manifest.toOfflinePackageDto(
                    serverId = serverId,
                    createdAtMillis = nowMillis,
                    updatedAtMillis = nowMillis,
                ),
                manifest.assets.map { it.toOfflineAssetDto(updatedAtMillis = nowMillis) },
                itemSnapshot?.toOfflineItemSnapshotDto(),
            )
        }

    override suspend fun getPackage(packageId: String): OfflinePackageManifest? =
        withContext(Dispatchers.IO) {
            val offlinePackage = database.getOfflinePackage(packageId) ?: return@withContext null
            offlinePackage.toOfflinePackageManifest(database.getOfflineAssets(packageId))
        }

    override fun observePackage(packageId: String): Flow<OfflinePackageManifest?> =
        combine(
            database.observeOfflinePackage(packageId),
            database.observeOfflineAssets(packageId),
        ) { offlinePackage, assets -> offlinePackage?.toOfflinePackageManifest(assets) }

    override suspend fun getPackagesByItemId(itemId: String): List<OfflinePackageManifest> =
        withContext(Dispatchers.IO) {
            database.getOfflinePackagesByItemId(itemId).map { offlinePackage ->
                offlinePackage.toOfflinePackageManifest(database.getOfflineAssets(offlinePackage.packageId))
            }
        }

    override suspend fun getPackagesByServerId(serverId: String): List<OfflinePackageManifest> =
        withContext(Dispatchers.IO) {
            database.getOfflinePackagesByServerId(serverId).map { offlinePackage ->
                offlinePackage.toOfflinePackageManifest(database.getOfflineAssets(offlinePackage.packageId))
            }
        }

    override suspend fun getReadyItemSnapshotsByServerId(
        serverId: String
    ): List<OfflineItemSnapshot> =
        withContext(Dispatchers.IO) {
            database.getReadyOfflineItemSnapshotsByServerId(serverId).map { it.toOfflineItemSnapshot() }
        }

    override suspend fun getAllPackages(): List<OfflinePackageManifest> =
        withContext(Dispatchers.IO) {
            database.getOfflinePackages().map { offlinePackage ->
                offlinePackage.toOfflinePackageManifest(database.getOfflineAssets(offlinePackage.packageId))
            }
        }

    override suspend fun getInterruptedActivePublicVideoPackageIds(): List<String> =
        withContext(Dispatchers.IO) { database.getInterruptedActivePublicVideoPackageIds() }

    override suspend fun getCanceledPublicVideoPackageIdsWithTempPath(): List<String> =
        withContext(Dispatchers.IO) { database.getCanceledPublicVideoPackageIdsWithTempPath() }

    override suspend fun setPackageReadiness(
        packageId: String,
        readiness: OfflinePackageReadiness,
        nowMillis: Long,
    ) =
        withContext(Dispatchers.IO) {
            database.setOfflinePackageReadiness(
                packageId = packageId,
                readiness = readiness,
                updatedAtMillis = nowMillis,
            )
        }

    override suspend fun setAssetState(
        asset: OfflineAsset,
        status: OfflineAssetStatus,
        failure: OfflineDownloadFailure?,
        bytes: Long?,
        tempPath: String?,
        finalPath: String?,
        retryCount: Int,
        nowMillis: Long,
    ) =
        withContext(Dispatchers.IO) {
            database.setOfflineAssetState(
                assetId = asset.assetId,
                status = status,
                failureKind = failure?.kind,
                failureMessage = failure?.message,
                retryCount = retryCount,
                bytes = bytes,
                tempPath = tempPath,
                finalPath = finalPath,
                updatedAtMillis = nowMillis,
            )
        }

    override suspend fun markInterruptedActivePublicVideoAssets(nowMillis: Long): Int =
        withContext(Dispatchers.IO) { database.markInterruptedActivePublicVideoAssets(nowMillis) }

    override suspend fun clearCanceledPublicVideoTempPaths(nowMillis: Long): Int =
        withContext(Dispatchers.IO) { database.clearCanceledPublicVideoTempPaths(nowMillis) }

    override suspend fun deletePackage(packageId: String) =
        withContext(Dispatchers.IO) { database.deleteOfflinePackage(packageId) }
}
