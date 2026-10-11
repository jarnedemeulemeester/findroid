package dev.jdtech.jellyfin.models

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "serverAddresses",
    foreignKeys =
        [
            ForeignKey(
                entity = Server::class,
                parentColumns = arrayOf("id"),
                childColumns = arrayOf("serverId"),
                onDelete = ForeignKey.CASCADE,
            )
        ],
)
data class ServerAddress(
    @PrimaryKey val id: UUID,
    @ColumnInfo(index = true) val serverId: String,
    val address: String,
)
