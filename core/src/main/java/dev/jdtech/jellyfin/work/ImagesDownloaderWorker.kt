package dev.jdtech.jellyfin.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.jdtech.jellyfin.repository.JellyfinRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.UUID

@HiltWorker
class ImagesDownloaderWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted private val params: WorkerParameters,
    private val repository: JellyfinRepository,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val itemId = UUID.fromString(params.inputData.getString(KEY_ITEM_ID))
        downloadImages(itemId = itemId)
        return Result.success()
    }

    private suspend fun downloadImages(
        itemId: UUID,
    ) {
        withContext(Dispatchers.IO) {
            val item = repository.getItem(itemId) ?: return@withContext

            val basePath = "images/${item.id}"

            val baseDir = File(appContext.filesDir, basePath)

            // Do not download images if they are already present
            if (baseDir.exists()) return@withContext

            val client = OkHttpClient()
            val uris = mapOf(
                "primary" to item.images.primary,
                "backdrop" to item.images.backdrop,
            )

            try {
                baseDir.mkdirs()
            } catch (e: IOException) {
                Timber.e(e)
                return@withContext
            }

            for ((name, uri) in uris) {
                if (uri == null) {
                    continue
                }

                val request = Request.Builder().url(uri.toString()).build()

                val imageBytes = try {
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            Timber.e("Failed to download image: ${response.code}")
                            continue
                        }

                        response.body.bytes()
                    }
                } catch (e: IOException) {
                    Timber.e(e)
                    continue
                }

                try {
                    val file = File(appContext.filesDir, "$basePath/$name")
                    file.writeBytes(imageBytes)
                } catch (e: IOException) {
                    Timber.e(e)
                }
            }
        }
    }

    companion object {
        const val KEY_ITEM_ID = "KEY_ITEM_ID"
    }
}
