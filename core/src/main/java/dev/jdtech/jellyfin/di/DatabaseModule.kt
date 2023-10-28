package dev.jdtech.jellyfin.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.jdtech.jellyfin.database.ServerDatabase
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Singleton
    @Provides
    fun provideServerDatabaseDao(@ApplicationContext app: Context): ServerDatabaseDao {
        return Room.databaseBuilder(
            app.applicationContext,
            ServerDatabase::class.java,
            "servers",
        )
            .fallbackToDestructiveMigration()
            .allowMainThreadQueries()
            .build()
            .getServerDatabaseDao()
    }
}
