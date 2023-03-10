package dev.jdtech.jellyfin.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "trickPlayManifests")
data class TrickPlayManifestDto(
    @PrimaryKey
    val version: String,
    val itemId: UUID,
    val resolution: Int,
)

fun TrickPlayManifest.toTrickPlayManifestDto(itemId: UUID): TrickPlayManifestDto {
    return TrickPlayManifestDto(
        version = version,
        itemId = itemId,
        resolution = widthResolutions.max(),
    )
}
