package dev.jdtech.jellyfin.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.jdtech.jellyfin.connectivity.ConnectivityMonitor
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ConnectivityModule {
    @Provides
    @Singleton
    fun provideConnectivityMonitor(
        @ApplicationContext context: Context,
    ): ConnectivityMonitor = ConnectivityMonitor(context)
}