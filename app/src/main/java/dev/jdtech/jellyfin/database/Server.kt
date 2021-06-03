package dev.jdtech.jellyfin.database

import java.util.*

data class Server(
    val id: UUID,
    val name: String,
    val address: String
)