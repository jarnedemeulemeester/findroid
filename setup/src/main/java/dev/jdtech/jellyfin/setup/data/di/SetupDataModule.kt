package dev.jdtech.jellyfin.setup.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.jdtech.jellyfin.AppPreferences
import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.setup.data.SetupRepositoryImpl
import dev.jdtech.jellyfin.setup.domain.SetupRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SetupDataModule {
    @Singleton
    @Provides
    fun provideSetupRepository(
        jellyfinApi: JellyfinApi,
        serverDatabase: ServerDatabaseDao,
        appPreferences: AppPreferences,
    ): SetupRepository {
        return SetupRepositoryImpl(jellyfinApi = jellyfinApi, database = serverDatabase, appPreferences = appPreferences)
    }
}
