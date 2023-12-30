package dev.jdtech.jellyfin.models

import androidx.room.Embedded
import androidx.room.Relation

data class ServerWithAddressAndUser(
    @Embedded
    val server: Server,
    @Relation(
        parentColumn = "currentServerAddressId",
        entityColumn = "id",
    )
    val address: ServerAddress?,
    @Relation(
        parentColumn = "currentUserId",
        entityColumn = "id",
    )
    val user: User?,
)
