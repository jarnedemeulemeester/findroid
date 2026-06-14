package dev.jdtech.jellyfin.offline

import dev.jdtech.jellyfin.offline.download.OfflineAsset
import dev.jdtech.jellyfin.offline.download.OfflineAssetKind
import dev.jdtech.jellyfin.offline.download.OfflineAssetStatus
import dev.jdtech.jellyfin.offline.download.OfflineDownloadFailureKind
import dev.jdtech.jellyfin.offline.download.OfflinePackageManifest
import dev.jdtech.jellyfin.offline.download.OfflineStorageScope
import dev.jdtech.jellyfin.offline.storage.DirectFileAssetResult
import dev.jdtech.jellyfin.offline.storage.DirectFileAssetStore
import dev.jdtech.jellyfin.offline.storage.OfflineTempPackageCleaner
import dev.jdtech.jellyfin.offline.transfer.OfflineVideoPostProcessReport
import dev.jdtech.jellyfin.offline.transfer.OfflineVideoPostProcessor
import java.io.File
import dev.jdtech.jellyfin.repository.OfflinePackageRepository
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class OfflineDownloadRecovery
@Inject
constructor(
    private val offlinePackageRepository: OfflinePackageRepository,
    private val offlineDownloadWorkScheduler: OfflineDownloadWorkScheduler,
    private val offlineTempPackageCleaner: OfflineTempPackageCleaner,
    private val offlineReadyVideoRepairer: OfflineReadyVideoRepairer,
) {
    suspend fun markInterruptedDownloads(nowMillis: Long = System.currentTimeMillis()): Int {
        val interruptedPackageIds = offlinePackageRepository.getInterruptedActivePublicVideoPackageIds()
        Timber.i("Offline recovery found %d interrupted package(s)", interruptedPackageIds.size)
        if (interruptedPackageIds.isEmpty()) return 0

        val markedCount = offlinePackageRepository.markInterruptedActivePublicVideoAssets(nowMillis)
        Timber.i("Offline recovery marked %d interrupted asset(s)", markedCount)
        interruptedPackageIds.forEach { packageId ->
            refreshReadiness(packageId, nowMillis)
            offlineDownloadWorkScheduler.cancelPackageTransfer(packageId)
        }
        return markedCount
    }

    suspend fun resumeInterruptedDownloads(nowMillis: Long = System.currentTimeMillis()): Int {
        var resumedCount = 0
        offlinePackageRepository.getAllPackages().forEach { manifest ->
            val videoAsset = manifest.interruptedRetryPublicVideoAsset() ?: return@forEach
            offlinePackageRepository.setAssetState(
                asset = videoAsset,
                status = OfflineAssetStatus.QUEUED,
                failure = null,
                bytes = videoAsset.bytes,
                tempPath = videoAsset.tempPath,
                finalPath = null,
                retryCount = videoAsset.retryCount,
                nowMillis = nowMillis,
            )
            refreshReadiness(manifest.packageId, nowMillis)
            offlineDownloadWorkScheduler.enqueuePublicVideoTransfer(manifest.packageId)
            resumedCount++
        }
        Timber.i("Offline recovery resumed %d interrupted package(s)", resumedCount)
        return resumedCount
    }

    suspend fun cleanupCanceledDownloads(nowMillis: Long = System.currentTimeMillis()): Int {
        val canceledPackageIds = offlinePackageRepository.getCanceledPublicVideoPackageIdsWithTempPath()
        canceledPackageIds.forEach { packageId -> runCatching { offlineTempPackageCleaner.cleanupTempPackage(packageId) } }
        val cleanedCount = offlinePackageRepository.clearCanceledPublicVideoTempPaths(nowMillis)
        canceledPackageIds.forEach { packageId -> refreshReadiness(packageId, nowMillis) }
        Timber.i("Offline recovery cleaned %d canceled asset(s)", cleanedCount)
        return cleanedCount
    }

    suspend fun repairSeekabilityForReadyDownloads(nowMillis: Long = System.currentTimeMillis()): Int {
        var repairedCount = 0
        offlinePackageRepository.getAllPackages().forEach { manifest ->
            val videoAsset = manifest.readyPublicVideoAsset() ?: return@forEach
            when (val result = offlineReadyVideoRepairer.repairIfNeeded(manifest)) {
                null -> Unit
                is DirectFileAssetResult.Success -> {
                    if (result.value.report.remuxed) {
                        offlinePackageRepository.setAssetState(
                            asset = videoAsset,
                            status = OfflineAssetStatus.READY,
                            failure = null,
                            bytes = result.value.file.length(),
                            tempPath = null,
                            finalPath = result.value.file.absolutePath,
                            retryCount = videoAsset.retryCount,
                            nowMillis = nowMillis,
                        )
                        refreshReadiness(manifest.packageId, nowMillis)
                        repairedCount++
                    }
                }
                is DirectFileAssetResult.Failure -> {
                    Timber.w(
                        "Offline recovery could not remux package=%s file=%s failure=%s message=%s",
                        manifest.packageId,
                        manifest.projectedPath.displayName,
                        result.failure.kind,
                        result.failure.message,
                    )
                }
            }
        }
        Timber.i("Offline recovery remuxed %d ready public video asset(s)", repairedCount)
        return repairedCount
    }

    private suspend fun refreshReadiness(packageId: String, nowMillis: Long) {
        offlinePackageRepository.getPackage(packageId)?.readiness?.let { readiness ->
            offlinePackageRepository.setPackageReadiness(packageId, readiness, nowMillis)
        }
    }

    private fun OfflinePackageManifest.interruptedRetryPublicVideoAsset(): OfflineAsset? =
        publicVideoAsset()?.takeIf { asset ->
            asset.status == OfflineAssetStatus.RETRY_WAIT &&
                asset.failure?.kind == OfflineDownloadFailureKind.AppInterrupted
        }

    private fun OfflinePackageManifest.readyPublicVideoAsset(): OfflineAsset? =
        publicVideoAsset()?.takeIf { asset -> asset.status == OfflineAssetStatus.READY }

    private fun OfflinePackageManifest.publicVideoAsset(): OfflineAsset? =
        assets.firstOrNull { asset ->
            asset.kind == OfflineAssetKind.VIDEO &&
                asset.storageScope == OfflineStorageScope.PUBLIC_MEDIA
        }
}

interface OfflineReadyVideoRepairer {
    suspend fun repairIfNeeded(
        manifest: OfflinePackageManifest
    ): DirectFileAssetResult<OfflineReadyVideoRepair>?
}

class DirectFileOfflineReadyVideoRepairer(
    private val directFileAssetStore: DirectFileAssetStore,
    private val offlineVideoPostProcessor: OfflineVideoPostProcessor,
) : OfflineReadyVideoRepairer {
    override suspend fun repairIfNeeded(
        manifest: OfflinePackageManifest
    ): DirectFileAssetResult<OfflineReadyVideoRepair>? {
        val publishedFile =
            directFileAssetStore.existingPublishedPublicAsset(manifest.projectedPath) ?: return null
        return when (
            val result =
                offlineVideoPostProcessor.ensureSeekableMp4(
                    file = publishedFile,
                    label = "recovery:${manifest.packageId}",
                )
        ) {
            is DirectFileAssetResult.Success ->
                DirectFileAssetResult.Success(
                    OfflineReadyVideoRepair(file = publishedFile, report = result.value)
                )
            is DirectFileAssetResult.Failure -> result
        }
    }
}

data class OfflineReadyVideoRepair(
    val file: File,
    val report: OfflineVideoPostProcessReport,
)
