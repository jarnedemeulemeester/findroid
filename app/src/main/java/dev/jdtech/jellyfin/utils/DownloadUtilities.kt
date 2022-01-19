package dev.jdtech.jellyfin.utils

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.getSystemService
import dev.jdtech.jellyfin.models.DownloadMetadata
import dev.jdtech.jellyfin.models.DownloadRequestItem
import dev.jdtech.jellyfin.models.DownloadSeriesMetadata
import dev.jdtech.jellyfin.models.PlayerItem
import dev.jdtech.jellyfin.repository.JellyfinRepository
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.UserItemDataDto
import timber.log.Timber
import java.io.File
import java.util.UUID

var defaultStorage: File? = null

fun requestDownload(uri: Uri, downloadRequestItem: DownloadRequestItem, context: Context) {
    val downloadRequest = DownloadManager.Request(uri)
        .setTitle(downloadRequestItem.metadata.name)
        .setDescription("Downloading")
        .setDestinationUri(
            Uri.fromFile(
                File(
                    defaultStorage,
                    downloadRequestItem.itemId.toString()
                )
            )
        )
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
    if (!File(defaultStorage, downloadRequestItem.itemId.toString()).exists())
        downloadFile(downloadRequest, context)
    createMetadataFile(
        downloadRequestItem.metadata,
        downloadRequestItem.itemId)
}

private fun createMetadataFile(metadata: DownloadMetadata, itemId: UUID) {
    val metadataFile = File(defaultStorage, "${itemId}.metadata")

    metadataFile.writeText("") //This might be necessary to make sure that the metadata file is empty

    if (metadata.type == "Episode") {
        metadataFile.printWriter().use { out ->
            out.println(metadata.id)
            out.println(metadata.type.toString())
            out.println(metadata.seriesName.toString())
            out.println(metadata.name.toString())
            out.println(metadata.parentIndexNumber.toString())
            out.println(metadata.indexNumber.toString())
            out.println(metadata.playbackPosition.toString())
            out.println(metadata.playedPercentage.toString())
            out.println(metadata.seriesId.toString())
            out.println(metadata.played.toString())
            out.println(if (metadata.overview != null) metadata.overview.replace("\n", "\\n") else "")
        }
    } else if (metadata.type == "Movie") {
        metadataFile.printWriter().use { out ->
            out.println(metadata.id)
            out.println(metadata.type.toString())
            out.println(metadata.name.toString())
            out.println(metadata.playbackPosition.toString())
            out.println(metadata.playedPercentage.toString())
            out.println(metadata.played.toString())
            out.println(if (metadata.overview != null) metadata.overview.replace("\n", "\\n") else "")
        }
    }

}

private fun downloadFile(request: DownloadManager.Request, context: Context) {
    request.apply {
        setAllowedOverMetered(false)
        setAllowedOverRoaming(false)
    }
    context.getSystemService<DownloadManager>()?.enqueue(request)
}

fun loadDownloadLocation(context: Context) {
    defaultStorage = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
}

fun loadDownloadedEpisodes(): List<PlayerItem> {
    val items = mutableListOf<PlayerItem>()
    defaultStorage?.walk()?.forEach {
        if (it.isFile && it.extension == "") {
            try {
                val metadataFile = File(defaultStorage, "${it.name}.metadata").readLines()
                val metadata = parseMetadataFile(metadataFile)
                items.add(
                    PlayerItem(
                        name = metadata.name,
                        itemId = UUID.fromString(it.name),
                        mediaSourceId = "",
                        playbackPosition = metadata.playbackPosition!!,
                        mediaSourceUri = it.absolutePath,
                        metadata = metadata
                    )
                )
            } catch (e: Exception) {
                it.delete()
                Timber.e(e)
            }

        }
    }
    return items.toList()
}

fun itemIsDownloaded(itemId: UUID): Boolean {
    val file = File(defaultStorage!!, itemId.toString())
    if (file.isFile && file.extension == "") {
        if (File(defaultStorage, "${itemId}.metadata").exists()){
            return true
        }
    }
    return false
}

fun getDownloadPlayerItem(itemId: UUID): PlayerItem? {
    val file = File(defaultStorage!!, itemId.toString())
    try{
        val metadataFile = File(defaultStorage, "${file.name}.metadata").readLines()
        val metadata = parseMetadataFile(metadataFile)
        return PlayerItem(metadata.name, UUID.fromString(file.name), "", metadata.playbackPosition!!, file.absolutePath, metadata)
    } catch (e: Exception) {
        file.delete()
        Timber.e(e)
    }
    return null
}

fun deleteDownloadedEpisode(uri: String) {
    try {
        File(uri).delete()
        File("${uri}.metadata").delete()
    } catch (e: Exception) {
        Timber.e(e)
    }

}

fun postDownloadPlaybackProgress(uri: String, playbackPosition: Long, playedPercentage: Double) {
    try {
        val metadataFile = File("${uri}.metadata")
        val metadataArray = metadataFile.readLines().toMutableList()
        if (metadataArray[1] == "Episode") {
            metadataArray[6] = playbackPosition.toString()
            metadataArray[7] = playedPercentage.toString()
        } else if (metadataArray[1] == "Movie") {
            metadataArray[3] = playbackPosition.toString()
            metadataArray[4] = playedPercentage.toString()
        }
        Timber.d("PLAYEDPERCENTAGE $playedPercentage")
        metadataFile.writeText("") //This might be necessary to make sure that the metadata file is empty
        metadataFile.printWriter().use { out ->
            metadataArray.forEach {
                out.println(it)
            }
        }
    } catch (e: Exception) {
        Timber.e(e)
    }
}

fun downloadMetadataToBaseItemDto(metadata: DownloadMetadata): BaseItemDto {
    val userData = UserItemDataDto(
        playbackPositionTicks = metadata.playbackPosition ?: 0,
        playedPercentage = metadata.playedPercentage,
        isFavorite = false,
        playCount = 0,
        played = false
    )

    return BaseItemDto(
        id = metadata.id,
        type = metadata.type,
        seriesName = metadata.seriesName,
        name = metadata.name,
        parentIndexNumber = metadata.parentIndexNumber,
        indexNumber = metadata.indexNumber,
        userData = userData,
        seriesId = metadata.seriesId,
        overview = metadata.overview
    )
}

fun baseItemDtoToDownloadMetadata(item: BaseItemDto): DownloadMetadata {
    return DownloadMetadata(
        id = item.id,
        type = item.type,
        seriesName = item.seriesName,
        name = item.name,
        parentIndexNumber = item.parentIndexNumber,
        indexNumber = item.indexNumber,
        playbackPosition = item.userData?.playbackPositionTicks ?: 0,
        playedPercentage = item.userData?.playedPercentage,
        seriesId = item.seriesId,
        played = item.userData?.played,
        overview = item.overview
    )
}

fun downloadSeriesMetadataToBaseItemDto(metadata: DownloadSeriesMetadata): BaseItemDto {
    val userData = UserItemDataDto(
        playbackPositionTicks = 0,
        playedPercentage = 0.0,
        isFavorite = false,
        playCount = 0,
        played = false,
        unplayedItemCount = metadata.episodes.size
    )

    return BaseItemDto(
        id = metadata.itemId,
        type = "Series",
        name = metadata.name,
        userData = userData
    )
}

fun parseMetadataFile(metadataFile: List<String>): DownloadMetadata {
    if (metadataFile[1] == "Episode") {
        return DownloadMetadata(
            id = UUID.fromString(metadataFile[0]),
            type = metadataFile[1],
            seriesName = metadataFile[2],
            name = metadataFile[3],
            parentIndexNumber = metadataFile[4].toInt(),
            indexNumber = metadataFile[5].toInt(),
            playbackPosition = metadataFile[6].toLong(),
            playedPercentage = if (metadataFile[7] == "null") {
                null
            } else {
                metadataFile[7].toDouble()
            },
            seriesId = UUID.fromString(metadataFile[8]),
            played = metadataFile[9].toBoolean(),
            overview = metadataFile[10].replace("\\n", "\n")
        )
    } else {
        return DownloadMetadata(
            id = UUID.fromString(metadataFile[0]),
            type = metadataFile[1],
            name = metadataFile[2],
            playbackPosition = metadataFile[3].toLong(),
            playedPercentage = if (metadataFile[4] == "null") {
                null
            } else {
                metadataFile[4].toDouble()
            },
            played = metadataFile[5].toBoolean(),
            overview = metadataFile[6].replace("\\n", "\n")
        )
    }
}

suspend fun syncPlaybackProgress(jellyfinRepository: JellyfinRepository) {
    val items = loadDownloadedEpisodes()
    items.forEach {
        try {
            val localPlaybackProgress = it.metadata?.playbackPosition
            val localPlayedPercentage = it.metadata?.playedPercentage

            val item = jellyfinRepository.getItem(it.itemId)
            val remotePlaybackProgress = item.userData?.playbackPositionTicks?.div(10000)
            val remotePlayedPercentage = item.userData?.playedPercentage

            var playbackProgress: Long = 0
            var playedPercentage = 0.0

            if (it.metadata?.played == true || item.userData?.played == true){
                return@forEach
            }

            if (localPlaybackProgress != null) {
                if (localPlaybackProgress > playbackProgress) {
                    playbackProgress = localPlaybackProgress
                    playedPercentage = localPlayedPercentage!!
                }
            }
            if (remotePlaybackProgress != null) {
                if (remotePlaybackProgress > playbackProgress) {
                    playbackProgress = remotePlaybackProgress
                    playedPercentage = remotePlayedPercentage!!
                }
            }

            if (playbackProgress != 0.toLong()) {
                postDownloadPlaybackProgress(it.mediaSourceUri, playbackProgress, playedPercentage)
                jellyfinRepository.postPlaybackProgress(
                    it.itemId,
                    playbackProgress.times(10000),
                    true
                )
                Timber.d("Percentage: $playedPercentage")
            }
        } catch (e: Exception) {
            Timber.e(e)
        }

    }
}