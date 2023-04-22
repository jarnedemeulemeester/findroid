package dev.jdtech.jellyfin.models

import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.repository.JellyfinRepository
import java.util.UUID
import org.jellyfin.sdk.model.api.MediaProtocol
import org.jellyfin.sdk.model.api.MediaSourceInfo
import java.io.File

data class FindroidSource(
    val id: String,
    val name: String,
    val type: FindroidSourceType,
    val path: String,
    val size: Long,
    val mediaStreams: List<FindroidMediaStream>,
    val downloadId: Long? = null
)

suspend fun MediaSourceInfo.toFindroidSource(
    jellyfinRepository: JellyfinRepository,
    itemId: UUID
): FindroidSource {
    val path = when (protocol) {
        MediaProtocol.FILE -> {
            try {
                jellyfinRepository.getStreamUrl(itemId, id.orEmpty())
            } catch (e: Exception) {
                ""
            }
        }
        MediaProtocol.HTTP -> this.path.orEmpty()
        else -> ""
    }
    return FindroidSource(
        id = id.orEmpty(),
        name = name.orEmpty(),
        type = FindroidSourceType.REMOTE,
        path = path,
        size = size ?: 0,
        mediaStreams = mediaStreams?.map { it.toFindroidMediaStream(jellyfinRepository) } ?: emptyList()
    )
}

fun FindroidSourceDto.toFindroidSource(
    serverDatabaseDao: ServerDatabaseDao,
): FindroidSource {
    return FindroidSource(
        id = id,
        name = name,
        type = type,
        path = path,
        size = File(path).length(),
        mediaStreams = serverDatabaseDao.getMediaStreamsBySourceId(id).map { it.toFindroidMediaStream() },
        downloadId = downloadId,
    )
}

enum class FindroidSourceType {
    REMOTE,
    LOCAL,
}
