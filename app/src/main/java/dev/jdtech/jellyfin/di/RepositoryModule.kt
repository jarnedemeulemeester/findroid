package dev.jdtech.jellyfin.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.repository.JellyfinRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Singleton
    @Provides
    fun provideJellyfinRepository(
        jellyfinApi: JellyfinApi
    ): JellyfinRepository {
        return JellyfinRepositoryImpl(jellyfinApi)
    }
}
