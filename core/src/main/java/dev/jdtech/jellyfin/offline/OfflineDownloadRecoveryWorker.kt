package dev.jdtech.jellyfin.offline

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class OfflineDownloadRecoveryWorker
@AssistedInject
constructor(
    @Assisted appContext: Context,
    @Assisted workerParameters: WorkerParameters,
    private val offlineDownloadRecovery: OfflineDownloadRecovery,
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        offlineDownloadRecovery.markInterruptedDownloads()
        offlineDownloadRecovery.cleanupCanceledDownloads()
        offlineDownloadRecovery.repairSeekabilityForReadyDownloads()
        return Result.success()
    }
}
