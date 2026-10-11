package dev.jdtech.jellyfin.models

import androidx.room3.Embedded
import androidx.room3.Relation

data class ServerWithAddresses(
    @Embedded val server: Server,
    @Relation(parentColumns = ["id"], entityColumns = ["serverId"])
    val addresses: List<ServerAddress>,
    @Relation(parentColumns = ["currentUserId"], entityColumns = ["id"]) val user: User?,
)
