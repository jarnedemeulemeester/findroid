package dev.jdtech.jellyfin.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.jdtech.jellyfin.AppPreferences
import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {
    @Singleton
    @Provides
    fun provideJellyfinApi(
        @ApplicationContext application: Context,
        appPreferences: AppPreferences,
        database: ServerDatabaseDao,
    ): JellyfinApi {
        val jellyfinApi = JellyfinApi.getInstance(
            context = application,
            requestTimeout = appPreferences.requestTimeout,
            connectTimeout = appPreferences.connectTimeout,
            socketTimeout = appPreferences.socketTimeout,
        )

        val serverId = appPreferences.currentServer ?: return jellyfinApi

        val serverWithAddressAndUser = database.getServerWithAddressAndUser(serverId) ?: return jellyfinApi
        val serverAddress = serverWithAddressAndUser.address ?: return jellyfinApi
        val user = serverWithAddressAndUser.user

        jellyfinApi.apply {
            api.baseUrl = serverAddress.address
            api.accessToken = user?.accessToken
            userId = user?.id
        }

        return jellyfinApi
    }
}
