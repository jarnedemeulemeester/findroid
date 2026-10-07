package dev.jdtech.jellyfin.di

import android.app.Application
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.connectivity.ConnectivityMonitor
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.repository.JellyfinRepositoryImpl
import dev.jdtech.jellyfin.repository.JellyfinRepositoryOfflineImpl
import dev.jdtech.jellyfin.repository.JellyfinRepositoryRouter
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Singleton
    @Provides
    fun provideJellyfinRepositoryImpl(
        application: Application,
        jellyfinApi: JellyfinApi,
        serverDatabase: ServerDatabaseDao,
        appPreferences: AppPreferences,
        connectivityMonitor: ConnectivityMonitor,
    ): JellyfinRepositoryImpl {
        println("Creating new jellyfinRepositoryImpl")
        return JellyfinRepositoryImpl(
            application,
            jellyfinApi,
            serverDatabase,
            appPreferences,
            connectivityMonitor,
        )
    }

    @Singleton
    @Provides
    fun provideJellyfinRepositoryOfflineImpl(
        application: Application,
        jellyfinApi: JellyfinApi,
        serverDatabase: ServerDatabaseDao,
        appPreferences: AppPreferences,
    ): JellyfinRepositoryOfflineImpl {
        println("Creating new jellyfinRepositoryOfflineImpl")
        return JellyfinRepositoryOfflineImpl(
            application,
            jellyfinApi,
            serverDatabase,
            appPreferences,
        )
    }

    @Provides
    fun provideJellyfinRepository(
        jellyfinRepositoryImpl: JellyfinRepositoryImpl,
        jellyfinRepositoryOfflineImpl: JellyfinRepositoryOfflineImpl,
        connectivityMonitor: ConnectivityMonitor,
        appPreferences: AppPreferences,
    ): JellyfinRepository {
        println("Creating new JellyfinRepository")
        return JellyfinRepositoryRouter(
            jellyfinRepositoryImpl,
            jellyfinRepositoryOfflineImpl,
            connectivityMonitor,
            isManuallyOffline = { appPreferences.getValue(appPreferences.offlineMode) },
        )
    }
}
