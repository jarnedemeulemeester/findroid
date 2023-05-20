package dev.jdtech.jellyfin.di

import android.app.Application
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.jdtech.jellyfin.AppPreferences
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.utils.Downloader
import dev.jdtech.jellyfin.utils.DownloaderImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DownloaderModule {
    @Singleton
    @Provides
    fun provideDownloader(
        application: Application,
        serverDatabase: ServerDatabaseDao,
        jellyfinRepository: JellyfinRepository,
        appPreferences: AppPreferences,
    ): Downloader {
        return DownloaderImpl(application, serverDatabase, jellyfinRepository, appPreferences)
    }
}
