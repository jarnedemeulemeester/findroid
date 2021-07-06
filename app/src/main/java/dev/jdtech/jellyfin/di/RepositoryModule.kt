package dev.jdtech.jellyfin.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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
    fun provideJellyfinRepository(@ApplicationContext application: Context): JellyfinRepository {
        val jellyfinApi = JellyfinApi.getInstance(application, "")
        return JellyfinRepositoryImpl(jellyfinApi)
    }
}