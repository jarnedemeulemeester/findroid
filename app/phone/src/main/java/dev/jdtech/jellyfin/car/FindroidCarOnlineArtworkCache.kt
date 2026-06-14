package dev.jdtech.jellyfin.car

import android.net.Uri
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.FindroidSeason
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import timber.log.Timber

private const val CONNECT_TIMEOUT_MS = 10_000
private const val READ_TIMEOUT_MS = 20_000
private const val MAX_IMAGE_BYTES = 2 * 1024 * 1024

internal object FindroidCarOnlineArtworkCache {
    fun cachedPaths(item: FindroidItem, filesDir: File): List<String> =
        candidateFiles(item, filesDir).map { it.file }.filter { it.isFile }.map { it.absolutePath }

    fun ensureCached(item: FindroidItem, filesDir: File, accessToken: String?): List<String> {
        val candidates = candidateFiles(item, filesDir)
        val existing = candidates.map { it.file }.filter { it.isFile }
        if (existing.isNotEmpty()) return existing.map { it.absolutePath }

        candidates.forEach { candidate ->
            val uri = candidate.uri ?: return@forEach
            runCatching { download(uri, candidate.file, accessToken) }
                .onFailure { Timber.w(it, "Failed to cache Android Auto artwork for %s", item.id) }
            if (candidate.file.isFile) return listOf(candidate.file.absolutePath)
        }

        return emptyList()
    }

    private fun candidateFiles(item: FindroidItem, filesDir: File): List<Candidate> =
        buildList {
            add(Candidate(item.images.primary, File(filesDir, "car-artwork/${item.id}/primary")))
            add(Candidate(item.images.backdrop, File(filesDir, "car-artwork/${item.id}/backdrop")))
            if (item is FindroidEpisode || item is FindroidSeason) {
                add(
                    Candidate(
                        item.images.showPrimary,
                        File(filesDir, "car-artwork/${item.seriesArtworkId()}/primary"),
                    )
                )
                add(
                    Candidate(
                        item.images.showBackdrop,
                        File(filesDir, "car-artwork/${item.seriesArtworkId()}/backdrop"),
                    )
                )
            }
        }

    private fun FindroidItem.seriesArtworkId() =
        when (this) {
            is FindroidEpisode -> seriesId
            is FindroidSeason -> seriesId
            else -> id
        }

    private fun download(uri: Uri, file: File, accessToken: String?) {
        file.parentFile?.mkdirs()
        val tempFile = File(file.parentFile, "${file.name}.tmp")
        val requestUri = uri.withAccessToken(accessToken)
        val connection = URL(requestUri.toString()).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.instanceFollowRedirects = true
            if (!accessToken.isNullOrBlank()) {
                connection.setRequestProperty("X-Emby-Token", accessToken)
            }
            connection.connect()
            if (connection.responseCode !in 200..299) {
                Timber.w("Android Auto artwork request failed with HTTP %d", connection.responseCode)
                return
            }
            val contentType = connection.contentType.orEmpty()
            if (!contentType.startsWith("image/")) return
            connection.inputStream.use { input ->
                tempFile.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var total = 0
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        total += read
                        if (total > MAX_IMAGE_BYTES) return
                        output.write(buffer, 0, read)
                    }
                    if (total <= 0) return
                }
            }
            if (!tempFile.renameTo(file)) {
                tempFile.copyTo(file, overwrite = true)
                tempFile.delete()
            }
        } finally {
            connection.disconnect()
            if (tempFile.isFile) tempFile.delete()
        }
    }

    private fun Uri.withAccessToken(accessToken: String?): Uri {
        if (accessToken.isNullOrBlank()) return this
        if (!getQueryParameter("api_key").isNullOrBlank()) return this
        return buildUpon().appendQueryParameter("api_key", accessToken).build()
    }

    private data class Candidate(val uri: Uri?, val file: File)
}
