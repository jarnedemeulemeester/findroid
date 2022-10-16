package dev.jdtech.jellyfin.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "serverAddresses")
data class ServerAddress(
    @PrimaryKey
    val id: UUID,
    val serverId: String,
    val address: String
)