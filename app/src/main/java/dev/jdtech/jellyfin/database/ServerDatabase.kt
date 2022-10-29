package dev.jdtech.jellyfin.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Server::class], version = 1, exportSchema = false)
abstract class ServerDatabase : RoomDatabase() {
    abstract val serverDatabaseDao: ServerDatabaseDao
}
