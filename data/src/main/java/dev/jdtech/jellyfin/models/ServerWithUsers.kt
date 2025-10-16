package dev.jdtech.jellyfin.models

import androidx.room.Embedded
import androidx.room.Relation

data class ServerWithUsers(
    @Embedded
    val server: Server,
    @Relation(
        parentColumn = "id",
        entityColumn = "serverId",
    )
    val users: List<User>,
)
