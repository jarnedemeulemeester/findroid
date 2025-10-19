package dev.jdtech.jellyfin.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "sources",
)
data class JellyCastSourceDto(
    @PrimaryKey
    val id: String,
    val itemId: UUID,
    val name: String,
    val type: JellyCastSourceType,
    val path: String,
    val downloadId: Long? = null,
)

fun JellyCastSource.toJellyCastSourceDto(itemId: UUID, path: String): JellyCastSourceDto {
    return JellyCastSourceDto(
        id = id,
        itemId = itemId,
        name = name,
        type = JellyCastSourceType.LOCAL,
        path = path,
    )
}
