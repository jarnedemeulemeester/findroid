package dev.jdtech.jellyfin.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import dev.jdtech.jellyfin.models.DownloadItem

@Database(
    entities = [DownloadItem::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class DownloadDatabase : RoomDatabase() {
    abstract val downloadDatabaseDao: DownloadDatabaseDao
}