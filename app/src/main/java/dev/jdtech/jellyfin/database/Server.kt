package dev.jdtech.jellyfin.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "servers")
data class Server(
    @PrimaryKey
    val id: String,
    val name: String,
    val address: String,
    val userId: String,
    val userName: String,
    val accessToken: String,
)