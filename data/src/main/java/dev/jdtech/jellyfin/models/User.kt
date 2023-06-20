package dev.jdtech.jellyfin.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "users",
    foreignKeys = [
        ForeignKey(
            entity = Server::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("serverId"),
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class User(
    @PrimaryKey
    val id: UUID,
    val name: String,
    @ColumnInfo(index = true)
    val serverId: String,
    val accessToken: String? = null,
)
