package dev.jdtech.jellyfin.models

import androidx.room3.Embedded
import androidx.room3.Relation

data class ServerWithAddressesAndUsers(
    @Embedded val server: Server,
    @Relation(parentColumns = ["id"], entityColumns = ["serverId"])
    val addresses: List<ServerAddress>,
    @Relation(parentColumns = ["id"], entityColumns = ["serverId"]) val users: List<User>,
)
