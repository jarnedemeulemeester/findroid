package dev.jdtech.jellyfin.offline

import dev.jdtech.jellyfin.offline.download.OfflineDownloadFailure
import dev.jdtech.jellyfin.offline.download.OfflineDownloadFailureKind
import dev.jdtech.jellyfin.offline.download.OfflineAssetKind
import dev.jdtech.jellyfin.offline.download.OfflineAssetStatus
import dev.jdtech.jellyfin.offline.download.OfflinePackageManifest
import dev.jdtech.jellyfin.offline.download.OfflineItemSnapshot
import dev.jdtech.jellyfin.offline.download.OfflineStorageScope
import dev.jdtech.jellyfin.offline.storage.DirectFileAssetStore
import dev.jdtech.jellyfin.repository.OfflinePackageRepository

interface OfflineDownloadManager {
    suspend fun enqueueVideoPackage(
        serverId: String,
        manifest: OfflinePackageManifest,
        itemSnapshot: OfflineItemSnapshot? = null,
        nowMillis: Long = System.currentTimeMillis(),
    ): OfflineDownloadEnqueueResult

    suspend fun cancelVideoPackage(
        packageId: String,
        nowMillis: Long = System.currentTimeMillis(),
    )

    suspend fun deleteVideoPackage(packageId: String)
}

sealed interface OfflineDownloadEnqueueResult {
    data object Enqueued : OfflineDownloadEnqueueResult

    data class Failed(val failure: OfflineDownloadFailure) : OfflineDownloadEnqueueResult
}

class OfflineDownloadManagerImpl(
    private val offlineDownloadCoordinator: OfflineDownloadCoordinator,
    private val offlineDownloadWorkScheduler: OfflineDownloadWorkScheduler,
    private val offlinePackageRepository: OfflinePackageRepository,
    private val directFileAssetStore: DirectFileAssetStore,
) : OfflineDownloadManager {
    override suspend fun enqueueVideoPackage(
        serverId: String,
        manifest: OfflinePackageManifest,
        itemSnapshot: OfflineItemSnapshot?,
        nowMillis: Long,
    ): OfflineDownloadEnqueueResult {
        val existingManifest = offlinePackageRepository.getPackage(manifest.packageId)
        if (existingManifest != null && !existingManifest.shouldReplaceForExplicitEnqueue()) {
            return OfflineDownloadEnqueueResult.Enqueued
        }
        if (existingManifest != null) {
            offlineDownloadWorkScheduler.cancelPackageTransfer(manifest.packageId)
            directFileAssetStore.cleanupTempPackage(manifest.packageId)
        }

        offlineDownloadCoordinator.enqueuePackage(
            serverId = serverId,
            manifest = manifest,
            itemSnapshot = itemSnapshot,
            nowMillis = nowMillis,
        )

        offlineDownloadWorkScheduler.enqueuePublicVideoTransfer(packageId = manifest.packageId)
        return OfflineDownloadEnqueueResult.Enqueued
    }

    override suspend fun cancelVideoPackage(packageId: String, nowMillis: Long) {
        val manifest = offlinePackageRepository.getPackage(packageId) ?: return
        val videoAsset =
            manifest.assets.firstOrNull {
                it.kind == OfflineAssetKind.VIDEO && it.storageScope == OfflineStorageScope.PUBLIC_MEDIA
            }
                ?: return
        offlinePackageRepository.setAssetState(
            asset = videoAsset,
            status = OfflineAssetStatus.FAILED_REQUIRED,
            failure = OfflineDownloadFailure(OfflineDownloadFailureKind.Canceled),
            bytes = videoAsset.bytes,
            tempPath = null,
            finalPath = null,
            retryCount = videoAsset.retryCount,
            nowMillis = nowMillis,
        )
        offlinePackageRepository.getPackage(packageId)?.readiness?.let { readiness ->
            offlinePackageRepository.setPackageReadiness(packageId, readiness, nowMillis)
        }
        offlineDownloadWorkScheduler.cancelPackageTransfer(packageId)
        runCatching { directFileAssetStore.cleanupTempPackage(packageId) }
    }

    override suspend fun deleteVideoPackage(packageId: String) {
        val manifest = offlinePackageRepository.getPackage(packageId) ?: return
        offlineDownloadWorkScheduler.cancelPackageTransfer(packageId)
        val referencedFinalPaths =
            offlinePackageRepository
                .getAllPackages()
                .asSequence()
                .filter { it.packageId != packageId }
                .flatMap { it.assets.asSequence() }
                .mapNotNull { it.finalPath }
                .toSet()
        manifest.assets.forEach { asset ->
            if (asset.finalPath != null && asset.finalPath in referencedFinalPaths) return@forEach
            runCatching {
                when (asset.storageScope) {
                    OfflineStorageScope.PUBLIC_MEDIA -> directFileAssetStore.deletePublicAsset(asset.finalPath)
                    OfflineStorageScope.APP_PRIVATE -> directFileAssetStore.deletePrivateAsset(asset.finalPath)
                    OfflineStorageScope.HIDDEN_WORK -> Unit
                }
            }
        }
        runCatching { directFileAssetStore.cleanupTempPackage(packageId) }
        offlinePackageRepository.deletePackage(packageId)
    }

    private fun OfflinePackageManifest.shouldReplaceForExplicitEnqueue(): Boolean {
        val videoAsset = publicVideoAsset() ?: return true
        if (
            videoAsset.status == OfflineAssetStatus.READY &&
                directFileAssetStore.existingPublishedPublicAsset(projectedPath) != null
        ) {
            return false
        }
        return videoAsset.status in RESTARTABLE_STATUSES
    }

    private fun OfflinePackageManifest.publicVideoAsset() =
        assets.firstOrNull {
            it.kind == OfflineAssetKind.VIDEO && it.storageScope == OfflineStorageScope.PUBLIC_MEDIA
        }

    private companion object {
        val RESTARTABLE_STATUSES =
            setOf(
                OfflineAssetStatus.RETRY_WAIT,
                OfflineAssetStatus.FAILED_REQUIRED,
            )
    }
}
