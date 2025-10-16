package dev.jdtech.jellyfin.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "serverAddresses",
    foreignKeys = [
        ForeignKey(
            entity = Server::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("serverId"),
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ServerAddress(
    @PrimaryKey
    val id: UUID,
    @ColumnInfo(index = true)
    val serverId: String,
    val address: String,
)
