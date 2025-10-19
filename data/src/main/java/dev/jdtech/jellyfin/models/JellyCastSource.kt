package dev.jdtech.jellyfin.models

import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.repository.JellyfinRepository
import org.jellyfin.sdk.model.api.MediaProtocol
import org.jellyfin.sdk.model.api.MediaSourceInfo
import java.io.File
import java.util.UUID

data class JellyCastSource(
    val id: String,
    val name: String,
    val type: JellyCastSourceType,
    val path: String,
    val size: Long,
    val mediaStreams: List<JellyCastMediaStream>,
    val downloadId: Long? = null,
)

suspend fun MediaSourceInfo.toJellyCastSource(
    jellyfinRepository: JellyfinRepository,
    itemId: UUID,
    includePath: Boolean = false,
): JellyCastSource {
    val path = when (protocol) {
        MediaProtocol.FILE -> {
            try {
                if (includePath) jellyfinRepository.getStreamUrl(itemId, id.orEmpty()) else ""
            } catch (e: Exception) {
                ""
            }
        }
        MediaProtocol.HTTP -> this.path.orEmpty()
        else -> ""
    }
    return JellyCastSource(
        id = id.orEmpty(),
        name = name.orEmpty(),
        type = JellyCastSourceType.REMOTE,
        path = path,
        size = size ?: 0,
        mediaStreams = mediaStreams?.map { it.toJellyCastMediaStream(jellyfinRepository) } ?: emptyList(),
    )
}

fun JellyCastSourceDto.toJellyCastSource(
    serverDatabaseDao: ServerDatabaseDao,
): JellyCastSource {
    return JellyCastSource(
        id = id,
        name = name,
        type = type,
        path = path,
        size = File(path).length(),
        mediaStreams = serverDatabaseDao.getMediaStreamsBySourceId(id).map { it.toJellyCastMediaStream() },
        downloadId = downloadId,
    )
}

enum class JellyCastSourceType {
    REMOTE,
    LOCAL,
}
