package dev.jdtech.jellyfin.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.jellyfin.sdk.model.api.MediaStreamType
import java.util.UUID

@Entity(
    tableName = "mediastreams",
)
data class FindroidMediaStreamDto(
    @PrimaryKey
    val id: UUID,
    val sourceId: String,
    val title: String,
    val displayTitle: String?,
    val language: String,
    val type: MediaStreamType,
    val codec: String,
    val isExternal: Boolean,
    val path: String,
    val channelLayout: String?,
    val videoRangeType: String?,
    val height: Int?,
    val width: Int?,
    val videoDoViTitle: String?,
    val downloadId: Long? = null,
)

fun FindroidMediaStream.toFindroidMediaStreamDto(id: UUID, sourceId: String, path: String): FindroidMediaStreamDto {
    return FindroidMediaStreamDto(
        id = id,
        sourceId = sourceId,
        title = title,
        displayTitle = displayTitle,
        language = language,
        type = type,
        codec = codec,
        isExternal = isExternal,
        path = path,
        channelLayout = channelLayout,
        videoRangeType = videoRangeType,
        height = height,
        width = width,
        videoDoViTitle = videoDoViTitle,
    )
}
