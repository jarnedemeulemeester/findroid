package dev.jdtech.jellyfin.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "parts")
data class FindroidPartDto(
    @PrimaryKey val id: UUID,
    val serverId: String?,
    val name: String,
    val originalTitle: String?,
    val overview: String,
    val runtimeTicks: Long,
    val chapters: List<FindroidChapter>?,
    val parentName: String?,
    val parentIndexNumber: Int?,
    val indexNumber: Int?,
    val indexNumberEnd: Int?,
)

fun FindroidPart.toFindroidPartDto(serverId: String? = null): FindroidPartDto {
    return FindroidPartDto(
        id = id,
        serverId = serverId,
        name = name,
        originalTitle = originalTitle,
        overview = overview,
        runtimeTicks = runtimeTicks,
        chapters = chapters,
        parentName = parentName,
        parentIndexNumber = parentIndexNumber,
        indexNumber = indexNumber,
        indexNumberEnd = indexNumberEnd,
    )
}
