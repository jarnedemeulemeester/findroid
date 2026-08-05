package dev.jdtech.jellyfin.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.jdtech.jellyfin.repository.JellyfinRepository
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber

@HiltWorker
class ImagesDownloaderWorker
@AssistedInject
constructor(
    @Assisted private val appContext: Context,
    @Assisted private val params: WorkerParameters,
    private val repository: JellyfinRepository,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val itemId = UUID.fromString(params.inputData.getString(KEY_ITEM_ID))
        downloadImages(itemId = itemId)
        return Result.success()
    }

    private suspend fun downloadImages(itemId: UUID) {
        withContext(Dispatchers.IO) {
            val item = repository.getItem(itemId) ?: return@withContext
            val basePath = "images/${item.id}"
            val baseDir = File(appContext.filesDir, basePath)

            val client = OkHttpClient()

            val imagesToDownload = mapOf(
                "primary" to item.images.primary,
                "backdrop" to item.images.backdrop,
                // "logo" to item.images.logo
            )

            try {
                baseDir.mkdirs()
            } catch (e: IOException) {
                Timber.e(e)
                return@withContext
            }

            for ((name, image) in imagesToDownload) {
                val uri = image?.uri ?: continue
                val file = File(baseDir, name)

                // Skip only if the individual file exists and is not empty
                if (file.exists() && file.length() > 0) continue

                Timber.d("Downloading image $name for item $itemId")

                val request = Request.Builder()
                    .url(uri.toString())
                    .build()

                val imageBytes =
                    try {
                        client.newCall(request).execute().use { response ->
                            if (response.isSuccessful) {
                                response.body.bytes()
                            } else {
                                Timber.e("Failed to download image: ${response.code}")
                                continue
                            }
                        }
                    } catch (e: IOException) {
                        Timber.e(e)
                        continue
                    }

                try {
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
