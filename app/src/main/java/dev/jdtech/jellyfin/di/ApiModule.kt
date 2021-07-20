package dev.jdtech.jellyfin.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.jdtech.jellyfin.api.JellyfinApi
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {
    @Singleton
    @Provides
    fun provideJellyfinApi(@ApplicationContext application: Context): JellyfinApi {
        return JellyfinApi.getInstance(application, "")
    }
}