package dev.jdtech.jellyfin.utils

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.text.format.Formatter
import androidx.core.net.toUri
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.FindroidMovie
import dev.jdtech.jellyfin.models.FindroidSource
import dev.jdtech.jellyfin.models.FindroidSources
import dev.jdtech.jellyfin.models.FindroidTrickplayInfo
import dev.jdtech.jellyfin.models.UiText
import dev.jdtech.jellyfin.models.toFindroidEpisodeDto
import dev.jdtech.jellyfin.models.toFindroidMediaStreamDto
import dev.jdtech.jellyfin.models.toFindroidMovieDto
import dev.jdtech.jellyfin.models.toFindroidSeasonDto
import dev.jdtech.jellyfin.models.toFindroidSegmentsDto
import dev.jdtech.jellyfin.models.toFindroidShowDto
import dev.jdtech.jellyfin.models.toFindroidSource
import dev.jdtech.jellyfin.models.toFindroidSourceDto
import dev.jdtech.jellyfin.models.toFindroidTrickplayInfoDto
import dev.jdtech.jellyfin.models.toFindroidUserDataDto
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlin.Exception
import kotlin.math.ceil
import dev.jdtech.jellyfin.core.R as CoreR

class DownloaderImpl(
    private val context: Context,
    private val database: ServerDatabaseDao,
    private val jellyfinRepository: JellyfinRepository,
    private val appPreferences: AppPreferences,
) : Downloader {
    private val downloadManager = context.getSystemService(DownloadManager::class.java)

    // TODO: We should probably move most (if not all) code to a worker.
    //  At this moment it is possible that some things are not downloaded due to the user leaving the current screen
    override suspend fun downloadItem(
        item: FindroidItem,
        sourceId: String,
        storageIndex: Int,
    ): Pair<Long, UiText?> = coroutineScope {
        try {
            val jobs = mutableListOf<Deferred<Unit>>()
            val source = jellyfinRepository.getMediaSources(item.id, true).first { it.id == sourceId }
            val segments = jellyfinRepository.getSegments(item.id)
            val trickplayInfo = if (item is FindroidSources) {
                item.trickplayInfo?.get(sourceId)
            } else {
                null
            }
            val storageLocation = context.getExternalFilesDirs(null)[storageIndex]
            if (storageLocation == null || Environment.getExternalStorageState(storageLocation) != Environment.MEDIA_MOUNTED) {
                return@coroutineScope Pair(-1, UiText.StringResource(CoreR.string.storage_unavailable))
            }
            val path =
                Uri.fromFile(File(storageLocation, "downloads/${item.id}.${source.id}.download"))
            val stats = StatFs(storageLocation.path)
            if (stats.availableBytes < source.size) {
                return@coroutineScope Pair(
                    -1,
                    UiText.StringResource(
                        CoreR.string.not_enough_storage,
                        Formatter.formatFileSize(context, source.size),
                        Formatter.formatFileSize(context, stats.availableBytes),
                    ),
                )
            }
            val request = DownloadManager.Request(source.path.toUri())
                .setTitle(item.name)
                .setAllowedOverMetered(appPreferences.getValue(appPreferences.downloadOverMobileData))
                .setAllowedOverRoaming(appPreferences.getValue(appPreferences.downloadWhenRoaming))
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationUri(path)
            val downloadId = downloadManager.enqueue(request)

            when (item) {
                is FindroidMovie -> {
                    database.insertMovie(item.toFindroidMovieDto(appPreferences.getValue(appPreferences.currentServer)))
                }
                is FindroidEpisode -> {
                    val show = jellyfinRepository.getShow(item.seriesId)
                    database.insertShow(show.toFindroidShowDto(appPreferences.getValue(appPreferences.currentServer)))
                    val season = jellyfinRepository.getSeason(item.seasonId)
                    database.insertSeason(season.toFindroidSeasonDto())
                    database.insertEpisode(item.toFindroidEpisodeDto(appPreferences.getValue(appPreferences.currentServer)))

                    jobs.add(async { downloadImages(show) })
                    jobs.add(async { downloadImages(season) })
                }
            }

            val sourceDto = source.toFindroidSourceDto(item.id, path.path.orEmpty())

            database.insertSource(sourceDto.copy(downloadId = downloadId))
            database.insertUserData(item.toFindroidUserDataDto(jellyfinRepository.getUserId()))

            downloadExternalMediaStreams(item, source, storageIndex)

            segments.forEach {
                database.insertSegment(it.toFindroidSegmentsDto(item.id))
            }

            if (trickplayInfo != null) {
                downloadTrickplayData(item.id, sourceId, trickplayInfo)
            }

            jobs.add(async { downloadImages(item) })

            jobs.awaitAll()

            return@coroutineScope Pair(downloadId, null)
        } catch (e: Exception) {
            try {
                val source = jellyfinRepository.getMediaSources(item.id).first { it.id == sourceId }
                deleteItem(item, source)
            } catch (_: Exception) {}

            return@coroutineScope Pair(-1, if (e.message != null) UiText.DynamicString(e.message!!) else UiText.StringResource(CoreR.string.unknown_error))
        }
    }

    override suspend fun cancelDownload(item: FindroidItem, downloadId: Long) {
        val source = database.getSourceByDownloadId(downloadId)?.toFindroidSource(database) ?: return
        if (source.downloadId != null) {
            downloadManager.remove(source.downloadId!!)
        }
        deleteItem(item, source)
    }

    override suspend fun deleteItem(item: FindroidItem, source: FindroidSource) {
        when (item) {
            is FindroidMovie -> {
                database.deleteMovie(item.id)
            }
            is FindroidEpisode -> {
                database.deleteEpisode(item.id)
                val remainingEpisodes = database.getEpisodesBySeasonId(item.seasonId)
                if (remainingEpisodes.isEmpty()) {
                    database.deleteSeason(item.seasonId)
                    database.deleteUserData(item.seasonId)
                    File(context.filesDir, "trickplay/${item.seasonId}").deleteRecursively()
                    val remainingSeasons = database.getSeasonsByShowId(item.seriesId)
                    if (remainingSeasons.isEmpty()) {
                        database.deleteShow(item.seriesId)
                        database.deleteUserData(item.seriesId)
                        File(context.filesDir, "trickplay/${item.seriesId}").deleteRecursively()
                    }
                }
            }
        }

        database.deleteSource(source.id)
        File(source.path).delete()

        val mediaStreams = database.getMediaStreamsBySourceId(source.id)
        for (mediaStream in mediaStreams) {
            File(mediaStream.path).delete()
        }
        database.deleteMediaStreamsBySourceId(source.id)

        database.deleteUserData(item.id)

        File(context.filesDir, "trickplay/${item.id}").deleteRecursively()
        File(context.filesDir, "images/${item.id}").deleteRecursively()
    }

    override suspend fun getProgress(downloadId: Long?): Pair<Int, Int> {
        var downloadStatus = -1
        var progress = -1
        if (downloadId == null) {
            return Pair(downloadStatus, progress)
        }
        val query = DownloadManager.Query()
            .setFilterById(downloadId)
        val cursor = downloadManager.query(query)
        if (cursor.moveToFirst()) {
            downloadStatus = cursor.getInt(
                cursor.getColumnIndexOrThrow(
                    DownloadManager.COLUMN_STATUS,
                ),
            )
            when (downloadStatus) {
                DownloadManager.STATUS_RUNNING -> {
                    val totalBytes = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    if (totalBytes > 0) {
                        val downloadedBytes = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                        progress = downloadedBytes.times(100).div(totalBytes).toInt()
                    }
                }
                DownloadManager.STATUS_SUCCESSFUL -> {
                    progress = 100
                }
            }
        } else {
            downloadStatus = DownloadManager.STATUS_FAILED
        }
        return Pair(downloadStatus, progress)
    }

    private fun downloadExternalMediaStreams(
        item: FindroidItem,
        source: FindroidSource,
        storageIndex: Int = 0,
    ) {
        val storageLocation = context.getExternalFilesDirs(null)[storageIndex]
        for (mediaStream in source.mediaStreams.filter { it.isExternal }) {
            val id = UUID.randomUUID()
            val streamPath = Uri.fromFile(File(storageLocation, "downloads/${item.id}.${source.id}.$id.download"))
            database.insertMediaStream(mediaStream.toFindroidMediaStreamDto(id, source.id, streamPath.path.orEmpty()))
            val request = DownloadManager.Request(mediaStream.path!!.toUri())
                .setTitle(mediaStream.title)
                .setAllowedOverMetered(appPreferences.getValue(appPreferences.downloadOverMobileData))
                .setAllowedOverRoaming(appPreferences.getValue(appPreferences.downloadWhenRoaming))
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)
                .setDestinationUri(streamPath)
            val downloadId = downloadManager.enqueue(request)
            database.setMediaStreamDownloadId(id, downloadId)
        }
    }

    private suspend fun downloadTrickplayData(
        itemId: UUID,
        sourceId: String,
        trickplayInfo: FindroidTrickplayInfo,
    ) {
        val maxIndex = ceil(trickplayInfo.thumbnailCount.toDouble().div(trickplayInfo.tileWidth * trickplayInfo.tileHeight)).toInt()
        val byteArrays = mutableListOf<ByteArray>()
        for (i in 0..maxIndex) {
            jellyfinRepository.getTrickplayData(
                itemId,
                trickplayInfo.width,
                i,
            )?.let { byteArray ->
                byteArrays.add(byteArray)
            }
        }
        saveTrickplayData(itemId, sourceId, trickplayInfo, byteArrays)
    }

    private fun saveTrickplayData(
        itemId: UUID,
        sourceId: String,
        trickplayInfo: FindroidTrickplayInfo,
        byteArrays: List<ByteArray>,
    ) {
        val basePath = "trickplay/$itemId/$sourceId"
        database.insertTrickplayInfo(trickplayInfo.toFindroidTrickplayInfoDto(sourceId))
        File(context.filesDir, basePath).mkdirs()
        for ((i, byteArray) in byteArrays.withIndex()) {
            val file = File(context.filesDir, "$basePath/$i")
            file.writeBytes(byteArray)
        }
    }

    private suspend fun downloadImages(
        item: FindroidItem,
    ) {
        withContext(Dispatchers.IO) {
            val client = OkHttpClient()
            val uris = mapOf(
                "primary" to item.images.primary,
                "backdrop" to item.images.backdrop,
                "showPrimary" to item.images.showPrimary,
                "showBackdrop" to item.images.showBackdrop,
            )
            val basePath = "images/${item.id}"

            try {
                File(context.filesDir, basePath).mkdirs()
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
                    val file = File(context.filesDir, "$basePath/$name")
                    file.writeBytes(imageBytes)
                } catch (e: IOException) {
                    Timber.e(e)
                }
            }
        }
    }
}
