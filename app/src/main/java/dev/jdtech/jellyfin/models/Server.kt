package dev.jdtech.jellyfin.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "servers")
data class Server(
    @PrimaryKey
    val id: String,
    val name: String,
    var currentServerAddressId: UUID?,
    var currentUserId: UUID?,
)
