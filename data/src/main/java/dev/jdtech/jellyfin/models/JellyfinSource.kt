package dev.jdtech.jellyfin.models

import dev.jdtech.jellyfin.repository.JellyfinRepository
import java.util.UUID
import org.jellyfin.sdk.model.api.MediaProtocol
import org.jellyfin.sdk.model.api.MediaSourceInfo
import org.jellyfin.sdk.model.api.MediaStream

data class JellyfinSource(
    val id: String,
    val name: String,
    val type: JellyfinSourceType,
    val path: String,
    val mediaStreams: List<MediaStream>,
    val downloadId: Long? = null
)

suspend fun MediaSourceInfo.toJellyfinSource(
    jellyfinRepository: JellyfinRepository? = null,
    itemId: UUID
): JellyfinSource {
    val path = when (protocol) {
        MediaProtocol.FILE -> {
            try {
                jellyfinRepository?.getStreamUrl(itemId, id.orEmpty()) ?: ""
            } catch (e: Exception) {
                ""
            }
        }
        MediaProtocol.HTTP -> this.path.orEmpty()
        else -> ""
    }
    return JellyfinSource(
        id = id.orEmpty(),
        name = name.orEmpty(),
        type = JellyfinSourceType.REMOTE,
        path = path,
        mediaStreams = mediaStreams ?: emptyList()
    )
}

fun FindroidSourceDto.toJellyfinSource(): JellyfinSource {
    return JellyfinSource(
        id = id,
        name = name,
        type = type,
        path = path,
        mediaStreams = emptyList(),
        downloadId = downloadId,
    )
}

enum class JellyfinSourceType {
    REMOTE,
    LOCAL,
}
