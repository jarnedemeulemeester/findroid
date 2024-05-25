package dev.jdtech.jellyfin.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "trickPlayManifests")
data class TrickPlayManifestDto(
    @PrimaryKey
    val itemId: UUID,
    val version: String,
    val resolution: Int,
)
