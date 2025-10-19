package dev.jdtech.jellyfin.models

import androidx.room.Entity
import java.util.UUID

@Entity(
    tableName = "userdata",
    primaryKeys = ["userId", "itemId"],
)
data class JellyCastUserDataDto(
    val userId: UUID,
    val itemId: UUID,
    val played: Boolean,
    val favorite: Boolean,
    val playbackPositionTicks: Long,
    val toBeSynced: Boolean = false,
)

fun JellyCastItem.toJellyCastUserDataDto(userId: UUID): JellyCastUserDataDto {
    return JellyCastUserDataDto(
        userId = userId,
        itemId = id,
        played = played,
        favorite = favorite,
        playbackPositionTicks = playbackPositionTicks,
    )
}
