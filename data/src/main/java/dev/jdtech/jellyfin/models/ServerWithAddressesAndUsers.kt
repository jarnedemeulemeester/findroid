package dev.jdtech.jellyfin.models

import androidx.room.Embedded
import androidx.room.Relation

data class ServerWithAddressesAndUsers(
    @Embedded
    val server: Server,
    @Relation(
        parentColumn = "id",
        entityColumn = "serverId",
    )
    val addresses: List<ServerAddress>,
    @Relation(
        parentColumn = "id",
        entityColumn = "serverId",
    )
    val users: List<User>,
)
