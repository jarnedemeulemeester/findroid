package dev.jdtech.jellyfin.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "userdata")
data class FindroidUserDataDto(
    @PrimaryKey
    val id: UUID,
    val userId: UUID,
    val itemId: UUID,
    val played: Boolean,
    val favorite: Boolean,
    val playbackPositionTicks: Long,
)

fun FindroidItem.toFindroidUserDataDto(userId: UUID): FindroidUserDataDto {
    return FindroidUserDataDto(
        id = UUID.randomUUID(),
        userId = userId,
        itemId = id,
        played = played,
        favorite = favorite,
        playbackPositionTicks = playbackPositionTicks,
    )
}
