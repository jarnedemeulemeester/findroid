package dev.jdtech.jellyfin.offline

import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.hilt.work.HiltWorker
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.jdtech.jellyfin.offline.download.OfflineAssetKind
import dev.jdtech.jellyfin.offline.download.OfflineAssetRequiredness
import dev.jdtech.jellyfin.offline.download.OfflineAssetStatus
import dev.jdtech.jellyfin.offline.download.OfflineDownloadFailure
import dev.jdtech.jellyfin.offline.download.OfflineDownloadFailureDisposition
import dev.jdtech.jellyfin.offline.download.OfflineDownloadFailureKind
import dev.jdtech.jellyfin.offline.download.OfflinePackageManifest
import dev.jdtech.jellyfin.offline.download.OfflineStorageScope
import dev.jdtech.jellyfin.offline.artwork.OfflineArtworkDownloader
import dev.jdtech.jellyfin.offline.storage.DirectFileAssetResult
import dev.jdtech.jellyfin.offline.storage.DirectFileAssetStore
import dev.jdtech.jellyfin.repository.OfflinePackageRepository
import dev.jdtech.jellyfin.repository.OfflineTransferPlanResult
import dev.jdtech.jellyfin.repository.OfflineTransferPlanner
import java.util.concurrent.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

@HiltWorker
class OfflineDownloadWorker
@AssistedInject
constructor(
    @Assisted appContext: Context,
    @Assisted workerParameters: WorkerParameters,
    private val offlinePackageRepository: OfflinePackageRepository,
    private val offlineDownloadCoordinator: OfflineDownloadCoordinator,
    private val offlineTransferPlanner: OfflineTransferPlanner,
    private val directFileAssetStore: DirectFileAssetStore,
    private val offlineArtworkDownloader: OfflineArtworkDownloader,
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        val packageId =
            inputData.getString(KEY_PACKAGE_ID)
                ?: return Result.failure(
                    failureData(OfflineDownloadFailure(OfflineDownloadFailureKind.MissingRequiredAsset))
                )
        val manifest =
            offlinePackageRepository.getPackage(packageId)
                ?: return Result.failure(
                    failureData(OfflineDownloadFailure(OfflineDownloadFailureKind.MissingRequiredAsset))
                )
        if (manifest.publicVideoAssetIsAlreadyPublished()) {
            offlineArtworkDownloader.downloadPackageArtwork(manifest.packageId)
            return Result.success()
        }
        val retryCountBeforeAttempt = manifest.videoRetryCount().coerceAtLeast(runAttemptCount)
        val retryCountAfterFailure = retryCountBeforeAttempt + 1
        return try {
            runVideoTransfer(manifest, retryCountAfterFailure, retryCountBeforeAttempt)
        } catch (exception: CancellationException) {
            withContext(NonCancellable) {
                markCanceledIfVideoIsNotReady(manifest, retryCountAfterFailure)
                runCatching { directFileAssetStore.cleanupTempPackage(manifest.packageId) }
            }
            throw exception
        }
    }

    private suspend fun runVideoTransfer(
        manifest: OfflinePackageManifest,
        retryCountAfterFailure: Int,
        retryCountBeforeAttempt: Int,
    ): Result {
        if (!applicationContext.isAppInForeground()) {
            markVideoAssetFailure(
                manifest = manifest,
                failure = OfflineDownloadFailure(OfflineDownloadFailureKind.AppInterrupted),
                retryCount = retryCountAfterFailure,
                retryExhausted = false,
            )
            return Result.success()
        }
        setForeground(createForegroundInfo(manifest.packageId))
        val plan =
            when (
                val planResult =
                    offlineTransferPlanner.planVideoTransfer(
                        itemId = manifest.itemId,
                        mediaSourceId = manifest.mediaSourceId,
                        profile = manifest.profile,
                    )
            ) {
                is OfflineTransferPlanResult.Success -> planResult.plan
                is OfflineTransferPlanResult.Failure -> {
                    markVideoAssetFailure(
                        manifest = manifest,
                        failure = planResult.failure,
                        retryCount = retryCountAfterFailure,
                        retryExhausted = planResult.failure.isRetryExhausted(retryCountAfterFailure),
                    )
                    return planResult.failure.toWorkResult(retryCountAfterFailure)
                }
            }

        return when (
            val result =
                offlineDownloadCoordinator.transferPublicVideoAsset(
                    manifest = manifest,
                    request = plan.request,
                    expectedBytes = plan.expectedBytes,
                    nowMillis = System.currentTimeMillis(),
                    attemptRetryCount = retryCountBeforeAttempt,
                    failureRetryCount = retryCountAfterFailure,
                )
            ) {
            is DirectFileAssetResult.Success -> {
                offlineArtworkDownloader.downloadPackageArtwork(manifest.packageId)
                Result.success()
            }
            is DirectFileAssetResult.Failure -> {
                if (result.failure.isRetryExhausted(retryCountAfterFailure)) {
                    markVideoAssetFailure(
                        manifest = manifest,
                        failure = result.failure,
                        retryCount = retryCountAfterFailure,
                        retryExhausted = true,
                    )
                }
                result.failure.toWorkResult(retryCountAfterFailure)
            }
        }
    }

    private suspend fun markCanceledIfVideoIsNotReady(
        manifest: OfflinePackageManifest,
        retryCount: Int,
    ) {
        val latestManifest = offlinePackageRepository.getPackage(manifest.packageId) ?: manifest
        if (latestManifest.publicVideoAssetIsAlreadyPublished()) return
        markVideoAssetFailure(
            manifest = latestManifest,
            failure = OfflineDownloadFailure(OfflineDownloadFailureKind.Canceled),
            retryCount = retryCount,
            retryExhausted = true,
            clearTempPath = true,
        )
    }

    private fun OfflineDownloadFailure.toWorkResult(retryCountAfterFailure: Int): Result =
        when {
            disposition == OfflineDownloadFailureDisposition.Retryable &&
                retryCountAfterFailure < MAX_RETRY_ATTEMPTS -> Result.retry()
            disposition == OfflineDownloadFailureDisposition.Retryable ->
                Result.failure(failureData(copy(message = message ?: "Retry limit reached")))
            disposition == OfflineDownloadFailureDisposition.UserAction ->
                Result.failure(failureData(this))
            else -> Result.failure(failureData(this))
        }

    private suspend fun markVideoAssetFailure(
        manifest: OfflinePackageManifest,
        failure: OfflineDownloadFailure,
        retryCount: Int,
        retryExhausted: Boolean,
        clearTempPath: Boolean = false,
    ) {
        val videoAsset = manifest.publicVideoAsset() ?: return
        val nowMillis = System.currentTimeMillis()
        offlinePackageRepository.setAssetState(
            asset = videoAsset,
            status = failure.toAssetFailureStatus(videoAsset.requiredness, retryExhausted),
            failure = failure,
            bytes = null,
            tempPath = if (clearTempPath) null else videoAsset.tempPath,
            finalPath = null,
            retryCount = retryCount,
            nowMillis = nowMillis,
        )
        offlinePackageRepository.getPackage(manifest.packageId)?.readiness?.let { readiness ->
            offlinePackageRepository.setPackageReadiness(manifest.packageId, readiness, nowMillis)
        }
    }

    private fun OfflineDownloadFailure.toAssetFailureStatus(
        requiredness: OfflineAssetRequiredness,
        retryExhausted: Boolean,
    ): OfflineAssetStatus =
        if (disposition == OfflineDownloadFailureDisposition.Retryable && !retryExhausted) {
            OfflineAssetStatus.RETRY_WAIT
        } else if (requiredness == OfflineAssetRequiredness.OPTIONAL) {
            OfflineAssetStatus.FAILED_OPTIONAL
        } else {
            OfflineAssetStatus.FAILED_REQUIRED
        }

    private fun OfflineDownloadFailure.isRetryExhausted(retryCountAfterFailure: Int): Boolean =
        disposition == OfflineDownloadFailureDisposition.Retryable && retryCountAfterFailure >= MAX_RETRY_ATTEMPTS

    private fun OfflinePackageManifest.videoRetryCount(): Int =
        publicVideoAsset()?.retryCount ?: 0

    private fun OfflinePackageManifest.publicVideoAssetIsAlreadyPublished(): Boolean {
        val videoAsset = publicVideoAsset() ?: return false
        return videoAsset.status == OfflineAssetStatus.READY &&
            directFileAssetStore.existingPublishedPublicAsset(projectedPath) != null
    }

    private fun OfflinePackageManifest.publicVideoAsset() =
        assets.firstOrNull {
            it.kind == OfflineAssetKind.VIDEO && it.storageScope == OfflineStorageScope.PUBLIC_MEDIA
        }

    private fun failureData(failure: OfflineDownloadFailure): Data =
        Data.Builder()
            .putString(KEY_FAILURE_KIND, failure.kind.name)
            .also { builder -> failure.message?.let { builder.putString(KEY_FAILURE_MESSAGE, it) } }
            .build()

    private fun createForegroundInfo(packageId: String): ForegroundInfo {
        ensureNotificationChannel()
        val cancelIntent =
            PendingIntent.getBroadcast(
                applicationContext,
                packageId.hashCode(),
                OfflineDownloadCancelReceiver.intent(applicationContext, packageId),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val notification =
            NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle("Findroid offline download")
                .setContentText("Preparing offline video")
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setProgress(0, 0, true)
                .addAction(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    "Cancel",
                    cancelIntent,
                )
                .build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel =
            NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Offline downloads",
                NotificationManager.IMPORTANCE_LOW,
            )
        notificationManager.createNotificationChannel(channel)
    }

    private fun Context.isAppInForeground(): Boolean {
        val processInfo = ActivityManager.RunningAppProcessInfo()
        ActivityManager.getMyMemoryState(processInfo)
        return processInfo.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
    }

    companion object {
        const val KEY_PACKAGE_ID = "packageId"
        const val KEY_FAILURE_KIND = "failureKind"
        const val KEY_FAILURE_MESSAGE = "failureMessage"
        private const val MAX_RETRY_ATTEMPTS = 5
        private const val NOTIFICATION_CHANNEL_ID = "offline_downloads"
        private const val NOTIFICATION_ID = 4201
    }
}
