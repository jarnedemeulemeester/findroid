package dev.jdtech.jellyfin.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "sources"
)
data class FindroidSourceDto(
    @PrimaryKey
    val id: String,
    val itemId: UUID,
    val name: String,
    val type: JellyfinSourceType,
    val path: String,
    val downloadId: Long? = null,
)

fun FindroidSource.toFindroidSourceDto(itemId: UUID, path: String): FindroidSourceDto {
    return FindroidSourceDto(
        id = id,
        itemId = itemId,
        name = name,
        type = JellyfinSourceType.LOCAL,
        path = path,
    )
}
