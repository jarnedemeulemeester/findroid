package dev.jdtech.jellyfin.di

import android.app.Application
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.repository.JellyfinRepositoryImpl
import dev.jdtech.jellyfin.repository.JellyfinRepositoryOfflineImpl
import dev.jdtech.jellyfin.repository.JellyfinOfflinePackagePlanner
import dev.jdtech.jellyfin.repository.JellyfinOfflineTransferPlanner
import dev.jdtech.jellyfin.repository.OfflinePackagePlanner
import dev.jdtech.jellyfin.repository.OfflinePackageRepository
import dev.jdtech.jellyfin.repository.OfflinePackageRepositoryImpl
import dev.jdtech.jellyfin.repository.OfflineTransferPlanner
import dev.jdtech.jellyfin.offline.download.OfflinePackageManifestFactory
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
    ): JellyfinRepositoryImpl {
        println("Creating new jellyfinRepositoryImpl")
        return JellyfinRepositoryImpl(application, jellyfinApi, serverDatabase, appPreferences)
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
        appPreferences: AppPreferences,
    ): JellyfinRepository {
        println("Creating new JellyfinRepository")
        return when (appPreferences.getValue(appPreferences.offlineMode)) {
            true -> jellyfinRepositoryOfflineImpl
            false -> jellyfinRepositoryImpl
        }
    }

    @Singleton
    @Provides
    fun provideOfflinePackageRepository(
        serverDatabase: ServerDatabaseDao
    ): OfflinePackageRepository = OfflinePackageRepositoryImpl(serverDatabase)

    @Singleton
    @Provides
    fun provideOfflineTransferPlanner(jellyfinApi: JellyfinApi): OfflineTransferPlanner =
        JellyfinOfflineTransferPlanner(jellyfinApi)

    @Singleton
    @Provides
    fun provideOfflinePackagePlanner(
        jellyfinApi: JellyfinApi,
        manifestFactory: OfflinePackageManifestFactory,
    ): OfflinePackagePlanner = JellyfinOfflinePackagePlanner(jellyfinApi, manifestFactory)
}
