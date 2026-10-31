package dev.jdtech.jellyfin.work

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber

/**
 * Downloads a media file in the app process.
 *
 * This replaces the system DownloadManager, which runs in a separate app that (starting with
 * Android 17) lacks the ACCESS_LOCAL_NETWORK permission and can therefore not reach servers on the
 * local network.
 */
@HiltWorker
class MediaDownloadWorker
@AssistedInject
constructor(
    @Assisted private val appContext: Context,
    @Assisted private val params: WorkerParameters,
    private val database: ServerDatabaseDao,
) : CoroutineWorker(appContext, params) {
    private val url = params.inputData.getString(KEY_URL).orEmpty()
    private val path = params.inputData.getString(KEY_PATH).orEmpty()
    private val title = params.inputData.getString(KEY_TITLE).orEmpty()
    private val downloadId = params.inputData.getLong(KEY_DOWNLOAD_ID, -1L)
    private val showNotification = params.inputData.getBoolean(KEY_SHOW_NOTIFICATION, false)
    private val itemId = params.inputData.getString(KEY_ITEM_ID)
    private val itemKind = params.inputData.getString(KEY_ITEM_KIND)

    private val notificationId = downloadId.hashCode()

    /**
     * Holds the ETag (or Last-Modified date) of the file on the server, which is sent along when
     * resuming so a file that changed in the meantime is downloaded again from the start.
     */
    private val validatorFile = File(appContext.cacheDir, "downloads/$downloadId.validator")

    private var madeProgress = false

    private enum class Outcome {
        SUCCESS,
        /** The download was cancelled or deleted before it could be finished. */
        REMOVED,
        RETRY,
        FAILURE,
    }

    override suspend fun doWork(): Result {
        if (url.isBlank() || path.isBlank() || downloadId == -1L) {
            return Result.failure()
        }

        if (showNotification) {
            createNotificationChannel()
            showProgress(0, 0)
        }

        val outcome =
            try {
                withContext(Dispatchers.IO) {
                    try {
                        download()
                    } catch (e: CancellationException) {
                        withContext(NonCancellable) { deleteFilesIfRemoved() }
                        throw e
                    } catch (e: IOException) {
                        Timber.e(e)
                        retryOrFail()
                    } catch (e: Exception) {
                        Timber.e(e)
                        Outcome.FAILURE
                    }
                }
            } finally {
                // Only the foreground service removes its notification by itself
                if (showNotification) {
                    NotificationManagerCompat.from(appContext).cancel(notificationId)
                }
            }

        if (outcome == Outcome.RETRY) {
            return Result.retry()
        }

        stalledAttempts.remove(downloadId)
        validatorFile.delete()

        // Nobody is waiting for the result of a download which got cancelled
        val notify = showNotification && !isStopped
        return when (outcome) {
            Outcome.SUCCESS -> {
                if (notify) notifyFinished(CoreR.string.download_complete)
                Result.success()
            }
            Outcome.REMOVED -> Result.success()
            else -> {
                if (notify) notifyFinished(CoreR.string.download_failed)
                Result.failure()
            }
        }
    }

    private suspend fun download(): Outcome {
        val file = File(path)
        file.parentFile?.mkdirs()
        val existingBytes = if (file.exists()) file.length() else 0L

        val request =
            Request.Builder()
                .url(url)
                // The byte counts have to match the file on disk to be able to resume
                .header("Accept-Encoding", "identity")
                .apply {
                    if (existingBytes > 0) {
                        header("Range", "bytes=$existingBytes-")
                        readValidator()?.let { header("If-Range", it) }
                    }
                }
                .build()

        client.newCall(request).execute().use { response ->
            if (response.code == 416) {
                // "Content-Range: bytes */<size>": nothing is left to download when the file was
                // already complete (e.g. the app was killed right before finalizing)
                val serverSize =
                    response.header("Content-Range")?.substringAfter('/')?.toLongOrNull()
                if (serverSize != existingBytes) {
                    // Partial file is not usable for resuming, start over
                    file.delete()
                    return retryOrFail()
                }
            } else {
                if (!response.isSuccessful) {
                    Timber.e("Failed to download $url: ${response.code}")
                    return if (response.code >= 500) retryOrFail() else Outcome.FAILURE
                }

                val append = response.code == 206
                if (!append) {
                    writeValidator(
                        response.header("ETag")?.takeUnless { it.startsWith("W/") }
                            ?: response.header("Last-Modified")
                    )
                }
                val offset = if (append) existingBytes else 0L
                val bodyLength = response.body.contentLength()
                val totalBytes = if (bodyLength >= 0) offset + bodyLength else -1L
                var downloadedBytes = offset
                var lastUpdate = 0L

                response.body.byteStream().use { input ->
                    FileOutputStream(file, append).use { output ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            downloadedBytes += read
                            madeProgress = true

                            val now = System.currentTimeMillis()
                            if (now - lastUpdate >= PROGRESS_INTERVAL_MS) {
                                lastUpdate = now
                                reportProgress(downloadedBytes, totalBytes)
                            }
                        }
                    }
                }

                if (totalBytes != -1L && downloadedBytes != totalBytes) {
                    throw IOException(
                        "Download ended early ($downloadedBytes of $totalBytes bytes)"
                    )
                }
            }
        }

        // Do not get interrupted between renaming the file and updating the database
        return withContext(NonCancellable) { finalizeDownload(file) }
    }

    /** Remove the `.download` extension and point the database to the final file. */
    private suspend fun finalizeDownload(file: File): Outcome {
        val finalFile = File(path.removeSuffix(".download"))

        val source = database.getSourceByDownloadId(downloadId)
        if (source != null) {
            if (!file.renameTo(finalFile)) return Outcome.FAILURE
            database.setSourcePath(source.id, finalFile.path)
            // The download may have been removed while the file was being renamed
            if (database.getSourceByDownloadId(downloadId) == null) {
                finalFile.delete()
                return Outcome.REMOVED
            }
            return Outcome.SUCCESS
        }

        val mediaStream = database.getMediaStreamByDownloadId(downloadId)
        if (mediaStream != null) {
            if (!file.renameTo(finalFile)) {
                database.deleteMediaStream(mediaStream.id)
                return Outcome.FAILURE
            }
            database.setMediaStreamPath(mediaStream.id, finalFile.path)
            if (database.getMediaStreamByDownloadId(downloadId) == null) {
                finalFile.delete()
                return Outcome.REMOVED
            }
            return Outcome.SUCCESS
        }

        // The download has been removed in the meantime
        file.delete()
        return Outcome.REMOVED
    }

    /**
     * Cancelling the work does not wait for this worker to stop, so the file may have been
     * (re)created after the download was removed. Clean up when nothing refers to it anymore.
     */
    private suspend fun deleteFilesIfRemoved() {
        if (
            database.getSourceByDownloadId(downloadId) == null &&
                database.getMediaStreamByDownloadId(downloadId) == null
        ) {
            File(path).delete()
            validatorFile.delete()
            stalledAttempts.remove(downloadId)
        }
    }

    private fun readValidator(): String? =
        try {
            validatorFile.readText().takeIf { it.isNotBlank() }
        } catch (_: IOException) {
            null
        }

    private fun writeValidator(validator: String?) {
        try {
            if (validator == null) {
                validatorFile.delete()
            } else {
                validatorFile.parentFile?.mkdirs()
                validatorFile.writeText(validator)
            }
        } catch (e: IOException) {
            Timber.w(e)
        }
    }

    private suspend fun reportProgress(downloadedBytes: Long, totalBytes: Long) {
        setProgress(
            workDataOf(KEY_DOWNLOADED_BYTES to downloadedBytes, KEY_TOTAL_BYTES to totalBytes)
        )
        if (showNotification && totalBytes > 0) {
            showProgress(downloadedBytes.times(100).div(totalBytes).toInt(), 100)
        }
    }

    /**
     * Run as a foreground service when possible. Starting one is not allowed while the app is in
     * the background (Android 12+), which is the case when the download resumes by itself after the
     * network came back. Fall back to a regular notification then.
     */
    private suspend fun showProgress(progress: Int, max: Int) {
        val foregroundInfo = createForegroundInfo(progress, max)
        try {
            setForeground(foregroundInfo)
        } catch (e: CancellationException) {
            throw e
        } catch (e: IllegalStateException) {
            Timber.w("Could not promote download to foreground: ${e.message}")
            if (canPostNotifications()) {
                NotificationManagerCompat.from(appContext)
                    .notify(notificationId, foregroundInfo.notification)
            }
        }
    }

    /** Keep retrying as long as progress is made, give up after too many attempts without any. */
    private fun retryOrFail(): Outcome {
        val stalled = if (madeProgress) 0 else (stalledAttempts[downloadId] ?: 0) + 1
        stalledAttempts[downloadId] = stalled
        return if (stalled < MAX_STALLED_ATTEMPTS) Outcome.RETRY else Outcome.FAILURE
    }

    private fun createNotificationChannel() {
        val channel =
            NotificationChannelCompat.Builder(
                    CHANNEL_ID,
                    NotificationManagerCompat.IMPORTANCE_LOW,
                )
                .setName(appContext.getString(CoreR.string.title_download))
                .build()
        NotificationManagerCompat.from(appContext).createNotificationChannel(channel)
    }

    /** Opens the item which is being downloaded in the launcher activity. */
    private fun createContentIntent(): PendingIntent? {
        if (itemId == null || itemKind == null) return null
        val component =
            appContext.packageManager.getLaunchIntentForPackage(appContext.packageName)?.component
                ?: return null
        val intent =
            Intent(ACTION_VIEW_ITEM)
                .setComponent(component)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
                .putExtra(EXTRA_ITEM_ID, itemId)
                .putExtra(EXTRA_ITEM_KIND, itemKind)
        return PendingIntent.getActivity(
            appContext,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createForegroundInfo(progress: Int, max: Int): ForegroundInfo {
        val notification =
            NotificationCompat.Builder(appContext, CHANNEL_ID)
                .setSmallIcon(CoreR.drawable.ic_download)
                .setContentTitle(title)
                .setContentText(appContext.getString(CoreR.string.download_downloading))
                .setContentIntent(createContentIntent())
                .setProgress(max, progress, max == 0)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                notificationId,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }

    private fun canPostNotifications(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

    private fun notifyFinished(text: Int) {
        if (!canPostNotifications()) return

        val notification: Notification =
            NotificationCompat.Builder(appContext, CHANNEL_ID)
                .setSmallIcon(CoreR.drawable.ic_download)
                .setContentTitle(title)
                .setContentText(appContext.getString(text))
                .setContentIntent(createContentIntent())
                .setAutoCancel(true)
                .build()

        // Use a different id than the foreground notification, which is removed when the work ends
        NotificationManagerCompat.from(appContext)
            .notify(finishedNotificationId(downloadId), notification)
    }

    companion object {
        const val KEY_URL = "KEY_URL"
        const val KEY_PATH = "KEY_PATH"
        const val KEY_TITLE = "KEY_TITLE"
        const val KEY_DOWNLOAD_ID = "KEY_DOWNLOAD_ID"
        const val KEY_SHOW_NOTIFICATION = "KEY_SHOW_NOTIFICATION"
        const val KEY_ITEM_ID = "KEY_ITEM_ID"
        const val KEY_ITEM_KIND = "KEY_ITEM_KIND"

        const val KEY_DOWNLOADED_BYTES = "KEY_DOWNLOADED_BYTES"
        const val KEY_TOTAL_BYTES = "KEY_TOTAL_BYTES"

        const val ACTION_VIEW_ITEM = "dev.jdtech.jellyfin.action.VIEW_ITEM"
        const val EXTRA_ITEM_ID = "dev.jdtech.jellyfin.extra.ITEM_ID"
        const val EXTRA_ITEM_KIND = "dev.jdtech.jellyfin.extra.ITEM_KIND"
        const val ITEM_KIND_MOVIE = "movie"
        const val ITEM_KIND_EPISODE = "episode"

        private const val CHANNEL_ID = "downloads"
        private const val BUFFER_SIZE = 64 * 1024
        private const val PROGRESS_INTERVAL_MS = 1000L
        private const val MAX_STALLED_ATTEMPTS = 5

        private val client by lazy { OkHttpClient() }

        /** Consecutive attempts per download id which did not download anything. */
        private val stalledAttempts = ConcurrentHashMap<Long, Int>()

        fun uniqueWorkName(downloadId: Long) = "media_download_$downloadId"

        fun finishedNotificationId(downloadId: Long) = downloadId.hashCode() + 1
    }
}
