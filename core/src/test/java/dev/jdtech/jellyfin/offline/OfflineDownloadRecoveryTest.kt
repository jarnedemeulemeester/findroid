package dev.jdtech.jellyfin.offline

import dev.jdtech.jellyfin.offline.download.OfflineAsset
import dev.jdtech.jellyfin.offline.download.OfflineAssetKind
import dev.jdtech.jellyfin.offline.download.OfflineAssetRequiredness
import dev.jdtech.jellyfin.offline.download.OfflineAssetStatus
import dev.jdtech.jellyfin.offline.download.OfflineDownloadFailure
import dev.jdtech.jellyfin.offline.download.OfflineDownloadFailureKind
import dev.jdtech.jellyfin.offline.download.OfflineItemSnapshot
import dev.jdtech.jellyfin.offline.download.OfflinePackageManifest
import dev.jdtech.jellyfin.offline.download.OfflinePackageReadiness
import dev.jdtech.jellyfin.offline.download.OfflineProfile
import dev.jdtech.jellyfin.offline.download.OfflineStorageScope
import dev.jdtech.jellyfin.offline.download.ProjectedPath
import dev.jdtech.jellyfin.offline.storage.OfflineTempPackageCleaner
import dev.jdtech.jellyfin.repository.OfflinePackageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OfflineDownloadRecoveryTest {
    @Test
    fun markInterruptedDownloadsCancelsActiveWorkAndDoesNotEnqueueTransfer() = runBlocking {
        val repository =
            FakeOfflinePackageRepository(
                manifest(
                    packageId = "package-1",
                    status = OfflineAssetStatus.DOWNLOADING,
                    retryCount = 2,
                )
        )
        val scheduler = FakeOfflineDownloadWorkScheduler()
        val tempPackageCleaner = FakeOfflineTempPackageCleaner()
        val recovery =
            OfflineDownloadRecovery(repository, scheduler, tempPackageCleaner, NoOpOfflineReadyVideoRepairer)

        val markedCount = recovery.markInterruptedDownloads(nowMillis = 100)

        val videoAsset = repository.videoAsset("package-1")
        assertEquals(1, markedCount)
        assertEquals(listOf("package-1"), scheduler.canceledPackageIds)
        assertEquals(emptyList<String>(), scheduler.enqueuedPackageIds)
        assertEquals(OfflineAssetStatus.RETRY_WAIT, videoAsset.status)
        assertEquals(OfflineDownloadFailureKind.AppInterrupted, videoAsset.failure?.kind)
        assertEquals(3, videoAsset.retryCount)
    }

    @Test
    fun markInterruptedDownloadsPersistsRetryWaitBeforeCancelingWork() = runBlocking {
        val repository =
            FakeOfflinePackageRepository(
                manifest(
                    packageId = "package-1",
                    status = OfflineAssetStatus.DOWNLOADING,
                )
            )
        val scheduler =
            FakeOfflineDownloadWorkScheduler(
                onCancelPackageTransfer = { packageId ->
                    assertEquals(OfflineAssetStatus.RETRY_WAIT, repository.videoAsset(packageId).status)
                    assertEquals(
                        OfflineDownloadFailureKind.AppInterrupted,
                        repository.videoAsset(packageId).failure?.kind,
                    )
                }
        )
        val tempPackageCleaner = FakeOfflineTempPackageCleaner()
        val recovery =
            OfflineDownloadRecovery(repository, scheduler, tempPackageCleaner, NoOpOfflineReadyVideoRepairer)

        recovery.markInterruptedDownloads(nowMillis = 100)

        assertEquals(listOf("package-1"), scheduler.canceledPackageIds)
    }

    @Test
    fun resumeInterruptedDownloadsEnqueuesOnlyAppInterruptedRetryWaitAssets() = runBlocking {
        val repository =
            FakeOfflinePackageRepository(
                manifest(
                    packageId = "interrupted",
                    status = OfflineAssetStatus.RETRY_WAIT,
                    failure = OfflineDownloadFailure(OfflineDownloadFailureKind.AppInterrupted),
                ),
                manifest(
                    packageId = "server-retry",
                    status = OfflineAssetStatus.RETRY_WAIT,
                    failure = OfflineDownloadFailure(OfflineDownloadFailureKind.Server5xx),
                ),
                manifest(
                    packageId = "active",
                    status = OfflineAssetStatus.DOWNLOADING,
                ),
        )
        val scheduler = FakeOfflineDownloadWorkScheduler()
        val tempPackageCleaner = FakeOfflineTempPackageCleaner()
        val recovery =
            OfflineDownloadRecovery(repository, scheduler, tempPackageCleaner, NoOpOfflineReadyVideoRepairer)

        val resumedCount = recovery.resumeInterruptedDownloads(nowMillis = 200)

        val resumedAsset = repository.videoAsset("interrupted")
        val serverRetryAsset = repository.videoAsset("server-retry")
        val activeAsset = repository.videoAsset("active")
        assertEquals(1, resumedCount)
        assertEquals(listOf("interrupted"), scheduler.enqueuedPackageIds)
        assertEquals(OfflineAssetStatus.QUEUED, resumedAsset.status)
        assertNull(resumedAsset.failure)
        assertEquals(OfflineAssetStatus.RETRY_WAIT, serverRetryAsset.status)
        assertEquals(OfflineDownloadFailureKind.Server5xx, serverRetryAsset.failure?.kind)
        assertEquals(OfflineAssetStatus.DOWNLOADING, activeAsset.status)
    }

    @Test
    fun cleanupCanceledDownloadsClearsStaleTempPathAndDeletesTempPackage() = runBlocking {
        val repository =
            FakeOfflinePackageRepository(
                manifest(
                    packageId = "canceled",
                    status = OfflineAssetStatus.FAILED_REQUIRED,
                    failure = OfflineDownloadFailure(OfflineDownloadFailureKind.Canceled),
                    tempPath = "/storage/emulated/0/Movies/Findroid/.findroid_tmp/canceled/video.part",
                    bytes = 123L,
                )
        )
        val scheduler = FakeOfflineDownloadWorkScheduler()
        val tempPackageCleaner = FakeOfflineTempPackageCleaner()
        val recovery =
            OfflineDownloadRecovery(repository, scheduler, tempPackageCleaner, NoOpOfflineReadyVideoRepairer)

        val cleanedCount = recovery.cleanupCanceledDownloads(nowMillis = 300)

        val videoAsset = repository.videoAsset("canceled")
        assertEquals(1, cleanedCount)
        assertEquals(listOf("canceled"), tempPackageCleaner.cleanedPackageIds)
        assertNull(videoAsset.tempPath)
        assertNull(videoAsset.bytes)
        assertEquals(OfflineAssetStatus.FAILED_REQUIRED, videoAsset.status)
        assertEquals(OfflineDownloadFailureKind.Canceled, videoAsset.failure?.kind)
    }

    private fun manifest(
        packageId: String,
        status: OfflineAssetStatus,
        failure: OfflineDownloadFailure? = null,
        retryCount: Int = 0,
        tempPath: String? = null,
        bytes: Long? = null,
    ): OfflinePackageManifest =
        OfflinePackageManifest(
            packageId = packageId,
            itemId = "$packageId-item",
            mediaSourceId = "$packageId-source",
            profile = OfflineProfile.Default480p,
            projectedPath = ProjectedPath(listOf("Library", packageId), "$packageId.mp4"),
            assets =
                listOf(
                    OfflineAsset(
                        assetId = "$packageId-video",
                        packageId = packageId,
                        kind = OfflineAssetKind.VIDEO,
                        ownerItemId = "$packageId-item",
                        sourceId = "$packageId-source",
                        profileId = OfflineProfile.Default480p.id,
                        mimeType = "video/mp4",
                        storageScope = OfflineStorageScope.PUBLIC_MEDIA,
                        requiredness = OfflineAssetRequiredness.PLAYBACK_REQUIRED,
                        status = status,
                        failure = failure,
                        tempPath = tempPath,
                        bytes = bytes,
                        retryCount = retryCount,
                    )
                ),
        )

    private class FakeOfflineDownloadWorkScheduler(
        private val onCancelPackageTransfer: (String) -> Unit = {},
    ) : OfflineDownloadWorkScheduler {
        val enqueuedPackageIds = mutableListOf<String>()
        val canceledPackageIds = mutableListOf<String>()

        override fun enqueuePublicVideoTransfer(packageId: String) {
            enqueuedPackageIds += packageId
        }

        override fun cancelPackageTransfer(packageId: String) {
            onCancelPackageTransfer(packageId)
            canceledPackageIds += packageId
        }
    }

    private class FakeOfflineTempPackageCleaner : OfflineTempPackageCleaner {
        val cleanedPackageIds = mutableListOf<String>()

        override fun cleanupTempPackage(packageId: String): Boolean {
            cleanedPackageIds += packageId
            return true
        }
    }

    private object NoOpOfflineReadyVideoRepairer : OfflineReadyVideoRepairer {
        override suspend fun repairIfNeeded(
            manifest: OfflinePackageManifest
        ): dev.jdtech.jellyfin.offline.storage.DirectFileAssetResult<OfflineReadyVideoRepair>? = null
    }

    private class FakeOfflinePackageRepository(
        vararg manifests: OfflinePackageManifest
    ) : OfflinePackageRepository {
        private val packages = manifests.associateBy { it.packageId }.toMutableMap()

        fun videoAsset(packageId: String): OfflineAsset =
            packages.getValue(packageId).assets.first { it.kind == OfflineAssetKind.VIDEO }

        override suspend fun savePackage(
            serverId: String,
            manifest: OfflinePackageManifest,
            itemSnapshot: OfflineItemSnapshot?,
            nowMillis: Long,
        ) {
            packages[manifest.packageId] = manifest
        }

        override suspend fun getPackage(packageId: String): OfflinePackageManifest? = packages[packageId]

        override fun observePackage(packageId: String): Flow<OfflinePackageManifest?> =
            flowOf(packages[packageId])

        override suspend fun getPackagesByItemId(itemId: String): List<OfflinePackageManifest> =
            packages.values.filter { it.itemId == itemId }

        override suspend fun getPackagesByServerId(serverId: String): List<OfflinePackageManifest> =
            packages.values.toList()

        override suspend fun getReadyItemSnapshotsByServerId(
            serverId: String
        ): List<OfflineItemSnapshot> = emptyList()

        override suspend fun getAllPackages(): List<OfflinePackageManifest> = packages.values.toList()

        override suspend fun getInterruptedActivePublicVideoPackageIds(): List<String> =
            packages.values
                .filter { manifest ->
                    manifest.assets.any { asset ->
                        asset.kind == OfflineAssetKind.VIDEO &&
                            asset.storageScope == OfflineStorageScope.PUBLIC_MEDIA &&
                            asset.status in INTERRUPTED_ACTIVE_STATUSES
                    }
                }
                .map { it.packageId }

        override suspend fun getCanceledPublicVideoPackageIdsWithTempPath(): List<String> =
            packages.values
                .filter { manifest ->
                    manifest.assets.any { asset ->
                        asset.kind == OfflineAssetKind.VIDEO &&
                            asset.storageScope == OfflineStorageScope.PUBLIC_MEDIA &&
                            asset.status == OfflineAssetStatus.FAILED_REQUIRED &&
                            asset.failure?.kind == OfflineDownloadFailureKind.Canceled &&
                            asset.tempPath != null
                    }
                }
                .map { it.packageId }

        override suspend fun setPackageReadiness(
            packageId: String,
            readiness: OfflinePackageReadiness,
            nowMillis: Long,
        ) = Unit

        override suspend fun setAssetState(
            asset: OfflineAsset,
            status: OfflineAssetStatus,
            failure: OfflineDownloadFailure?,
            bytes: Long?,
            tempPath: String?,
            finalPath: String?,
            retryCount: Int,
            nowMillis: Long,
        ) {
            val manifest = packages.getValue(asset.packageId)
            val updatedAsset =
                asset.copy(
                    status = status,
                    failure = failure,
                    bytes = bytes,
                    tempPath = tempPath,
                    finalPath = finalPath,
                    retryCount = retryCount,
                )
            packages[asset.packageId] =
                manifest.copy(
                    assets =
                        manifest.assets.map { currentAsset ->
                            if (currentAsset.assetId == asset.assetId) updatedAsset else currentAsset
                        }
                )
        }

        override suspend fun markInterruptedActivePublicVideoAssets(nowMillis: Long): Int {
            var markedCount = 0
            packages.replaceAll { _, manifest ->
                manifest.copy(
                    assets =
                        manifest.assets.map { asset ->
                            if (
                                asset.kind == OfflineAssetKind.VIDEO &&
                                    asset.storageScope == OfflineStorageScope.PUBLIC_MEDIA &&
                                    asset.status in INTERRUPTED_ACTIVE_STATUSES
                            ) {
                                markedCount++
                                asset.copy(
                                    status = OfflineAssetStatus.RETRY_WAIT,
                                    failure = OfflineDownloadFailure(OfflineDownloadFailureKind.AppInterrupted),
                                    finalPath = null,
                                    retryCount = asset.retryCount + 1,
                                )
                            } else {
                                asset
                            }
                        }
                )
            }
            return markedCount
        }

        override suspend fun clearCanceledPublicVideoTempPaths(nowMillis: Long): Int {
            var cleanedCount = 0
            packages.replaceAll { _, manifest ->
                manifest.copy(
                    assets =
                        manifest.assets.map { asset ->
                            if (
                                asset.kind == OfflineAssetKind.VIDEO &&
                                    asset.storageScope == OfflineStorageScope.PUBLIC_MEDIA &&
                                    asset.status == OfflineAssetStatus.FAILED_REQUIRED &&
                                    asset.failure?.kind == OfflineDownloadFailureKind.Canceled &&
                                    asset.tempPath != null
                            ) {
                                cleanedCount++
                                asset.copy(tempPath = null, bytes = null)
                            } else {
                                asset
                            }
                        }
                )
            }
            return cleanedCount
        }

        override suspend fun deletePackage(packageId: String) {
            packages.remove(packageId)
        }

        private companion object {
            val INTERRUPTED_ACTIVE_STATUSES =
                setOf(
                    OfflineAssetStatus.QUEUED,
                    OfflineAssetStatus.DOWNLOADING,
                    OfflineAssetStatus.VERIFYING,
                )
        }
    }
}
