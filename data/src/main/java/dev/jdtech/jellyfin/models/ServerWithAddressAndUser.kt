package dev.jdtech.jellyfin.models

import androidx.room3.Embedded
import androidx.room3.Relation

data class ServerWithAddressAndUser(
    @Embedded val server: Server,
    @Relation(parentColumns = ["currentServerAddressId"], entityColumns = ["id"])
    val address: ServerAddress?,
    @Relation(parentColumns = ["currentUserId"], entityColumns = ["id"]) val user: User?,
)
