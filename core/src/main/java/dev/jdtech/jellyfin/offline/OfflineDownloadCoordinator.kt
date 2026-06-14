package dev.jdtech.jellyfin.offline

import dev.jdtech.jellyfin.offline.download.OfflineAssetKind
import dev.jdtech.jellyfin.offline.download.OfflineAssetRequiredness
import dev.jdtech.jellyfin.offline.download.OfflineAssetStatus
import dev.jdtech.jellyfin.offline.download.OfflineDownloadFailure
import dev.jdtech.jellyfin.offline.download.OfflineDownloadFailureDisposition
import dev.jdtech.jellyfin.offline.download.OfflineDownloadFailureKind
import dev.jdtech.jellyfin.offline.download.OfflineDownloadState
import dev.jdtech.jellyfin.offline.download.OfflineDownloadEvent
import dev.jdtech.jellyfin.offline.download.OfflinePackageManifest
import dev.jdtech.jellyfin.offline.download.OfflinePackageReadiness
import dev.jdtech.jellyfin.offline.download.OfflineDownloadStateMachine
import dev.jdtech.jellyfin.offline.download.OfflineDownloadTransition
import dev.jdtech.jellyfin.offline.download.OfflineItemSnapshot
import dev.jdtech.jellyfin.offline.download.OfflineStorageScope
import dev.jdtech.jellyfin.offline.download.OfflineTransferRequest
import dev.jdtech.jellyfin.offline.storage.DirectFileAssetResult
import dev.jdtech.jellyfin.offline.storage.DirectFileAssetStore
import dev.jdtech.jellyfin.offline.storage.PreparedPublicAsset
import dev.jdtech.jellyfin.offline.transfer.OfflineAssetTransferRunner
import dev.jdtech.jellyfin.repository.OfflinePackageRepository
import java.io.File
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface OfflineDownloadCoordinator {
    suspend fun enqueuePackage(
        serverId: String,
        manifest: OfflinePackageManifest,
        itemSnapshot: OfflineItemSnapshot?,
        nowMillis: Long,
    ): OfflineDownloadTransition

    suspend fun preparePublicVideoAsset(
        manifest: OfflinePackageManifest
    ): DirectFileAssetResult<PreparedPublicAsset>

    suspend fun transferPublicVideoAsset(
        manifest: OfflinePackageManifest,
        request: OfflineTransferRequest,
        expectedBytes: Long?,
        nowMillis: Long,
        attemptRetryCount: Int,
        failureRetryCount: Int,
    ): DirectFileAssetResult<File>
}

class OfflineDownloadCoordinatorImpl(
    private val offlinePackageRepository: OfflinePackageRepository,
    private val directFileAssetStore: DirectFileAssetStore,
    private val offlineAssetTransferRunner: OfflineAssetTransferRunner,
    private val stateMachine: OfflineDownloadStateMachine = OfflineDownloadStateMachine(),
) : OfflineDownloadCoordinator {
    private val serialMutex = Mutex()

    override suspend fun enqueuePackage(
        serverId: String,
        manifest: OfflinePackageManifest,
        itemSnapshot: OfflineItemSnapshot?,
        nowMillis: Long,
    ): OfflineDownloadTransition =
        serialMutex.withLock {
            offlinePackageRepository.savePackage(
                serverId = serverId,
                manifest = manifest,
                itemSnapshot = itemSnapshot,
                nowMillis = nowMillis,
            )
            stateMachine.reduce(OfflineDownloadState(), OfflineDownloadEvent.StartRequested)
        }

    override suspend fun preparePublicVideoAsset(
        manifest: OfflinePackageManifest
    ): DirectFileAssetResult<PreparedPublicAsset> =
        serialMutex.withLock {
            val videoAsset =
                manifest.assets.firstOrNull {
                    it.kind == OfflineAssetKind.VIDEO &&
                        it.storageScope == OfflineStorageScope.PUBLIC_MEDIA
                }
                    ?: return@withLock DirectFileAssetResult.Failure(
                        OfflineDownloadFailure(OfflineDownloadFailureKind.MissingRequiredAsset)
                    )
            directFileAssetStore.preparePublicAsset(
                packageId = manifest.packageId,
                assetId = videoAsset.assetId,
                projectedPath = manifest.projectedPath,
            )
        }

    override suspend fun transferPublicVideoAsset(
        manifest: OfflinePackageManifest,
        request: OfflineTransferRequest,
        expectedBytes: Long?,
        nowMillis: Long,
        attemptRetryCount: Int,
        failureRetryCount: Int,
    ): DirectFileAssetResult<File> =
        serialMutex.withLock {
            val videoAsset =
                manifest.assets.firstOrNull {
                    it.kind == OfflineAssetKind.VIDEO &&
                        it.storageScope == OfflineStorageScope.PUBLIC_MEDIA
                }
                    ?: return@withLock DirectFileAssetResult.Failure(
                        OfflineDownloadFailure(OfflineDownloadFailureKind.MissingRequiredAsset)
                    )
            val recoveredPublishedAsset =
                if (videoAsset.status in RECOVERABLE_PUBLISHED_STATUSES) {
                    directFileAssetStore.existingPublishedPublicAsset(manifest.projectedPath)
                } else {
                    null
                }
            if (recoveredPublishedAsset != null) {
                val scanFailure =
                    when (val scanResult = directFileAssetStore.scanPublicAsset(recoveredPublishedAsset)) {
                        is DirectFileAssetResult.Success -> null
                        is DirectFileAssetResult.Failure -> scanResult.failure
                    }
                offlinePackageRepository.setAssetState(
                    asset = videoAsset,
                    status = OfflineAssetStatus.READY,
                    failure = scanFailure,
                    bytes = recoveredPublishedAsset.length(),
                    tempPath = null,
                    finalPath = recoveredPublishedAsset.absolutePath,
                    retryCount = attemptRetryCount,
                    nowMillis = nowMillis,
                )
                directFileAssetStore.cleanupTempPackage(manifest.packageId)
                refreshReadiness(manifest.packageId, nowMillis)
                return@withLock DirectFileAssetResult.Success(recoveredPublishedAsset)
            }
            val prepared =
                when (val result = preparePublicVideoAssetUnlocked(manifest, expectedBytes)) {
                    is DirectFileAssetResult.Success -> result.value
                    is DirectFileAssetResult.Failure -> {
                        offlinePackageRepository.setAssetState(
                            asset = videoAsset,
                            status = result.failure.toAssetFailureStatus(videoAsset.requiredness),
                            failure = result.failure,
                            bytes = null,
                            tempPath = null,
                            finalPath = null,
                            retryCount = failureRetryCount,
                            nowMillis = nowMillis,
                        )
                        refreshReadiness(manifest.packageId, nowMillis)
                        return@withLock result
                    }
                }

            offlinePackageRepository.setAssetState(
                asset = videoAsset,
                status = OfflineAssetStatus.DOWNLOADING,
                failure = null,
                bytes = null,
                tempPath = prepared.tempFile.absolutePath,
                finalPath = null,
                retryCount = attemptRetryCount,
                nowMillis = nowMillis,
            )

            var lastProgressUpdateMillis = 0L
            when (
                val transferResult =
                    offlineAssetTransferRunner.transferPublicAsset(
                        preparedAsset = prepared,
                        request = request,
                        expectedBytes = expectedBytes,
                        onBytesTransferred = { bytes ->
                            val progressNowMillis = System.currentTimeMillis()
                            if (progressNowMillis - lastProgressUpdateMillis >= PROGRESS_UPDATE_INTERVAL_MS) {
                                lastProgressUpdateMillis = progressNowMillis
                                offlinePackageRepository.setAssetState(
                                    asset = videoAsset,
                                    status = OfflineAssetStatus.DOWNLOADING,
                                    failure = null,
                                    bytes = bytes,
                                    tempPath = prepared.tempFile.absolutePath,
                                    finalPath = null,
                                    retryCount = attemptRetryCount,
                                    nowMillis = progressNowMillis,
                                )
                            }
                        },
                    )
            ) {
                is DirectFileAssetResult.Success -> {
                    offlinePackageRepository.setAssetState(
                        asset = videoAsset,
                        status = OfflineAssetStatus.READY,
                        failure = transferResult.value.scanFailure,
                        bytes = transferResult.value.file.length(),
                        tempPath = null,
                        finalPath = transferResult.value.file.absolutePath,
                        retryCount = attemptRetryCount,
                        nowMillis = nowMillis,
                    )
                    directFileAssetStore.cleanupTempPackage(manifest.packageId)
                    refreshReadiness(manifest.packageId, nowMillis)
                    DirectFileAssetResult.Success(transferResult.value.file)
                }
                is DirectFileAssetResult.Failure -> {
                    offlinePackageRepository.setAssetState(
                        asset = videoAsset,
                        status = transferResult.failure.toAssetFailureStatus(videoAsset.requiredness),
                        failure = transferResult.failure,
                        bytes = prepared.tempFile.takeIf { it.exists() }?.length(),
                        tempPath = prepared.tempFile.absolutePath,
                        finalPath = null,
                        retryCount = failureRetryCount,
                        nowMillis = nowMillis,
                    )
                    refreshReadiness(manifest.packageId, nowMillis)
                    transferResult
                }
            }
        }

    private companion object {
        const val PROGRESS_UPDATE_INTERVAL_MS = 1_000L
        val RECOVERABLE_PUBLISHED_STATUSES =
            setOf(
                OfflineAssetStatus.DOWNLOADING,
                OfflineAssetStatus.VERIFYING,
                OfflineAssetStatus.RETRY_WAIT,
            )
    }

    private fun preparePublicVideoAssetUnlocked(
        manifest: OfflinePackageManifest,
        expectedBytes: Long? = null,
    ): DirectFileAssetResult<PreparedPublicAsset> {
        val videoAsset =
            manifest.assets.firstOrNull {
                it.kind == OfflineAssetKind.VIDEO && it.storageScope == OfflineStorageScope.PUBLIC_MEDIA
            }
                ?: return DirectFileAssetResult.Failure(
                    OfflineDownloadFailure(OfflineDownloadFailureKind.MissingRequiredAsset)
                )
        return directFileAssetStore.preparePublicAsset(
            packageId = manifest.packageId,
            assetId = videoAsset.assetId,
            projectedPath = manifest.projectedPath,
            expectedBytes = expectedBytes,
        )
    }

    private suspend fun refreshReadiness(packageId: String, nowMillis: Long) {
        val readiness =
            offlinePackageRepository.getPackage(packageId)?.readiness
                ?: OfflinePackageReadiness.NOT_READY
        offlinePackageRepository.setPackageReadiness(packageId, readiness, nowMillis)
    }

    private fun OfflineDownloadFailure.toAssetFailureStatus(
        requiredness: OfflineAssetRequiredness
    ): OfflineAssetStatus =
        if (disposition == OfflineDownloadFailureDisposition.Retryable) {
            OfflineAssetStatus.RETRY_WAIT
        } else if (requiredness == OfflineAssetRequiredness.OPTIONAL) {
            OfflineAssetStatus.FAILED_OPTIONAL
        } else {
            OfflineAssetStatus.FAILED_REQUIRED
        }
}
