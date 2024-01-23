package dev.jdtech.jellyfin.models

import androidx.room.Embedded
import androidx.room.Relation

data class ServerWithAddresses(
    @Embedded
    val server: Server,
    @Relation(
        parentColumn = "id",
        entityColumn = "serverId",
    )
    val addresses: List<ServerAddress>,
    @Relation(
        parentColumn = "currentUserId",
        entityColumn = "id",
    )
    val user: User?,
)
