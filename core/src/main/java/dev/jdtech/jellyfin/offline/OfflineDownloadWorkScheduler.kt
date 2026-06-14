package dev.jdtech.jellyfin.offline

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

interface OfflineDownloadWorkScheduler {
    fun enqueuePublicVideoTransfer(packageId: String)

    fun cancelPackageTransfer(packageId: String)
}

class WorkManagerOfflineDownloadWorkScheduler(
    private val workManager: WorkManager,
) : OfflineDownloadWorkScheduler {
    override fun enqueuePublicVideoTransfer(packageId: String) {
        val workRequest =
            OneTimeWorkRequestBuilder<OfflineDownloadWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    RETRY_BACKOFF_MINUTES,
                    TimeUnit.MINUTES,
                )
                .setInputData(
                    workDataOf(
                        OfflineDownloadWorker.KEY_PACKAGE_ID to packageId,
                    )
                )
                .addTag(packageId)
                .build()

        workManager.enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            workRequest,
        )
    }

    override fun cancelPackageTransfer(packageId: String) {
        workManager.cancelAllWorkByTag(packageId)
    }

    companion object {
        const val UNIQUE_WORK_NAME = "offline_download_serial"
        private const val RETRY_BACKOFF_MINUTES = 1L
    }
}
