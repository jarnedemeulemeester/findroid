package dev.jdtech.jellyfin.di

import android.app.Application
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import dev.jdtech.jellyfin.utils.DatabaseDownloadedSources
import dev.jdtech.jellyfin.utils.DownloadForegroundService
import dev.jdtech.jellyfin.utils.DownloadQueue
import dev.jdtech.jellyfin.utils.DownloadQueueImpl
import dev.jdtech.jellyfin.utils.DatabaseDownloadQueueStore
import dev.jdtech.jellyfin.utils.QueuedItemLoader
import dev.jdtech.jellyfin.utils.QueuedItemType
import dev.jdtech.jellyfin.utils.Downloader
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DownloadQueueModule {
    @Singleton
    @Provides
    fun provideDownloadQueue(
        application: Application,
        serverDatabase: ServerDatabaseDao,
        appPreferences: AppPreferences,
        downloader: Downloader,
        jellyfinRepository: JellyfinRepository,
    ): DownloadQueue {
        return DownloadQueueImpl(
            downloader = downloader,
            sources = DatabaseDownloadedSources(serverDatabase),
            store = DatabaseDownloadQueueStore(serverDatabase),
            // The queue keeps only an id across a restart, so the item is fetched again here.
            // Both lookups fall back to the downloaded copy when the server no longer has it.
            loadItem =
                QueuedItemLoader { itemId, type ->
                    when (type) {
                        QueuedItemType.EPISODE -> jellyfinRepository.getEpisode(itemId)
                        QueuedItemType.MOVIE -> jellyfinRepository.getMovie(itemId)
                    }
                },
            isSequentialEnabled = {
                appPreferences.getValue(appPreferences.downloadSequential)
            },
            onWorkAccepted = { DownloadForegroundService.start(application) },
        )
    }
}
