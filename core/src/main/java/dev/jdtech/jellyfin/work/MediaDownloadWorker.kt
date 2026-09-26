package dev.jdtech.jellyfin.work

import android.Manifest
import android.app.Notification
import android.content.Context
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
import kotlinx.coroutines.Dispatchers
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

    private val notificationId = downloadId.hashCode()

    override suspend fun doWork(): Result {
        if (url.isBlank() || path.isBlank() || downloadId == -1L) {
            return Result.failure()
        }

        if (showNotification) {
            createNotificationChannel()
            try {
                setForeground(createForegroundInfo(0, 0))
            } catch (e: IllegalStateException) {
                // Starting a foreground service from the background is not allowed (Android 12+)
                Timber.w(e, "Could not promote download to foreground")
            }
        }

        return withContext(Dispatchers.IO) {
            try {
                download()
            } catch (e: IOException) {
                Timber.e(e)
                retryOrFail()
            }
        }
    }

    private suspend fun download(): Result {
        val file = File(path)
        file.parentFile?.mkdirs()
        val existingBytes = if (file.exists()) file.length() else 0L

        val request =
            Request.Builder()
                .url(url)
                .apply { if (existingBytes > 0) header("Range", "bytes=$existingBytes-") }
                .build()

        client.newCall(request).execute().use { response ->
            if (response.code == 416) {
                // Partial file is not usable for resuming, start over
                file.delete()
                return retryOrFail()
            }
            if (!response.isSuccessful) {
                Timber.e("Failed to download $url: ${response.code}")
                return if (response.code >= 500) retryOrFail() else Result.failure()
            }

            val append = response.code == 206
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

                        val now = System.currentTimeMillis()
                        if (now - lastUpdate >= PROGRESS_INTERVAL_MS) {
                            lastUpdate = now
                            reportProgress(downloadedBytes, totalBytes)
                        }
                    }
                }
            }

            if (totalBytes != -1L && downloadedBytes != totalBytes) {
                throw IOException("Download ended early ($downloadedBytes of $totalBytes bytes)")
            }
        }

        return if (finalizeDownload(file)) {
            if (showNotification) notifyComplete()
            Result.success()
        } else {
            Result.failure()
        }
    }

    /** Remove the `.download` extension and point the database to the final file. */
    private suspend fun finalizeDownload(file: File): Boolean {
        val finalFile = File(path.removeSuffix(".download"))

        val source = database.getSourceByDownloadId(downloadId)
        if (source != null) {
            if (!file.renameTo(finalFile)) return false
            database.setSourcePath(source.id, finalFile.path)
            return true
        }

        val mediaStream = database.getMediaStreamByDownloadId(downloadId)
        if (mediaStream != null) {
            if (!file.renameTo(finalFile)) {
                database.deleteMediaStream(mediaStream.id)
                return false
            }
            database.setMediaStreamPath(mediaStream.id, finalFile.path)
            return true
        }

        // The download has been removed in the meantime
        file.delete()
        return true
    }

    private suspend fun reportProgress(downloadedBytes: Long, totalBytes: Long) {
        setProgress(
            workDataOf(KEY_DOWNLOADED_BYTES to downloadedBytes, KEY_TOTAL_BYTES to totalBytes)
        )
        if (showNotification && totalBytes > 0) {
            val progress = downloadedBytes.times(100).div(totalBytes).toInt()
            try {
                setForeground(createForegroundInfo(progress, 100))
            } catch (_: IllegalStateException) {}
        }
    }

    private fun retryOrFail(): Result =
        if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()

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

    private fun createForegroundInfo(progress: Int, max: Int): ForegroundInfo {
        val notification =
            NotificationCompat.Builder(appContext, CHANNEL_ID)
                .setSmallIcon(CoreR.drawable.ic_download)
                .setContentTitle(title)
                .setContentText(appContext.getString(CoreR.string.download_downloading))
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

    private fun notifyComplete() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    appContext,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notification: Notification =
            NotificationCompat.Builder(appContext, CHANNEL_ID)
                .setSmallIcon(CoreR.drawable.ic_download)
                .setContentTitle(title)
                .setContentText(appContext.getString(CoreR.string.download_complete))
                .setAutoCancel(true)
                .build()

        // Use a different id than the foreground notification, which is removed when the work ends
        NotificationManagerCompat.from(appContext).notify(notificationId + 1, notification)
    }

    companion object {
        const val KEY_URL = "KEY_URL"
        const val KEY_PATH = "KEY_PATH"
        const val KEY_TITLE = "KEY_TITLE"
        const val KEY_DOWNLOAD_ID = "KEY_DOWNLOAD_ID"
        const val KEY_SHOW_NOTIFICATION = "KEY_SHOW_NOTIFICATION"

        const val KEY_DOWNLOADED_BYTES = "KEY_DOWNLOADED_BYTES"
        const val KEY_TOTAL_BYTES = "KEY_TOTAL_BYTES"

        private const val CHANNEL_ID = "downloads"
        private const val BUFFER_SIZE = 64 * 1024
        private const val PROGRESS_INTERVAL_MS = 1000L
        private const val MAX_ATTEMPTS = 5

        private val client by lazy { OkHttpClient() }

        fun uniqueWorkName(downloadId: Long) = "media_download_$downloadId"
    }
}
