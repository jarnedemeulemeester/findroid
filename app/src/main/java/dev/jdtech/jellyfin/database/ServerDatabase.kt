package dev.jdtech.jellyfin.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Server::class], version = 1, exportSchema = false)
abstract class ServerDatabase : RoomDatabase() {
    abstract val serverDatabaseDao: ServerDatabaseDao

    companion object {
        @Volatile
        private var INSTANCE: ServerDatabase? = null

        fun getInstance(context: Context): ServerDatabase {
            synchronized(this) {
                var instance = INSTANCE
                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        ServerDatabase::class.java,
                        "servers"
                    )
                        .fallbackToDestructiveMigration().build()
                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}