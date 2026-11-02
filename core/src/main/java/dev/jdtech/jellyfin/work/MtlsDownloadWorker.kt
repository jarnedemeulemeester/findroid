package dev.jdtech.jellyfin.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

@HiltWorker
class MtlsDownloadWorker
@AssistedInject
constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val database: ServerDatabaseDao,
    private val appPreferences: AppPreferences,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result =
        withContext(Dispatchers.IO) {
            val url = inputData.getString(KEY_URL) ?: return@withContext Result.failure()
            val temporaryPath =
                inputData.getString(KEY_TEMPORARY_PATH) ?: return@withContext Result.failure()
            val finalPath = temporaryPath.removeSuffix(".download")
            val serverId = appPreferences.getValue(appPreferences.currentServer)
            val accessToken = serverId?.let {
                database.getServerWithAddressAndUser(it)?.user?.accessToken
            }
            val requestBuilder = Request.Builder().url(url)
            accessToken?.let { requestBuilder.header("X-Emby-Token", it) }
            val request = requestBuilder.build()
            val temporaryFile = File(temporaryPath)

            try {
                JellyfinApi.getInstance(applicationContext)
                    .httpClient
                    .newCall(request)
                    .execute()
                    .use { response ->
                        if (!response.isSuccessful) {
                            return@withContext if (response.code in 500..599) {
                                Result.retry()
                            } else {
                                Result.failure()
                            }
                        }
                        val body = response.body ?: return@withContext Result.failure()
                        val totalBytes = body.contentLength()
                        temporaryFile.parentFile?.mkdirs()
                        BufferedInputStream(body.byteStream()).use { input ->
                            BufferedOutputStream(temporaryFile.outputStream()).use { output ->
                                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                                var downloadedBytes = 0L
                                var lastProgress = -1L
                                var read: Int
                                while (input.read(buffer).also { read = it } != -1) {
                                    if (isStopped) return@withContext Result.failure()
                                    output.write(buffer, 0, read)
                                    downloadedBytes += read
                                    if (totalBytes > 0) {
                                        val progress = downloadedBytes * 100 / totalBytes
                                        if (progress != lastProgress) {
                                            setProgress(workDataOf(KEY_PROGRESS to progress))
                                            lastProgress = progress
                                        }
                                    }
                                }
                            }
                        }
                    }

                if (!temporaryFile.renameTo(File(finalPath))) {
                    return@withContext Result.failure()
                }
                inputData.getString(KEY_SOURCE_ID)?.let { database.setSourcePath(it, finalPath) }
                inputData.getString(KEY_MEDIA_STREAM_ID)?.let {
                    database.setMediaStreamPath(UUID.fromString(it), finalPath)
                }
                Result.success()
            } catch (exception: IOException) {
                temporaryFile.delete()
                Result.retry()
            }
        }

    companion object {
        const val KEY_URL = "url"
        const val KEY_TEMPORARY_PATH = "temporary_path"
        const val KEY_SOURCE_ID = "source_id"
        const val KEY_MEDIA_STREAM_ID = "media_stream_id"
        const val KEY_PROGRESS = "progress"
    }
}
