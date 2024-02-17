package dev.jdtech.jellyfin.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "gestureProfiles")
data class GestureProfile(
    @PrimaryKey
    val id: UUID,
    val name: String,
)
