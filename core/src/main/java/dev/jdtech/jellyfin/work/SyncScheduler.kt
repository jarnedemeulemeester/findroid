package dev.jdtech.jellyfin.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Schedule syncing of locally stored user data (playback progress, played and favorite state) to
 * the server.
 *
 * @param restart Replace any pending or retrying sync, resetting its retry counter. Use when
 *   leaving offline mode. When false, an already queued or running sync is kept.
 */
fun scheduleUserDataSync(context: Context, restart: Boolean = false) {
    val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    val request =
        OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

    WorkManager.getInstance(context.applicationContext)
        .enqueueUniqueWork(
            uniqueWorkName = "syncUserData",
            existingWorkPolicy =
                if (restart) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
            request = request,
        )
}
