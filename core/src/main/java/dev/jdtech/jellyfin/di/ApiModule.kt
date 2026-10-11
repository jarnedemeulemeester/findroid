package dev.jdtech.jellyfin.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking

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
        val jellyfinApi =
            JellyfinApi.getInstance(
                context = application,
                requestTimeout = appPreferences.getValue(appPreferences.requestTimeout),
                connectTimeout = appPreferences.getValue(appPreferences.connectTimeout),
                socketTimeout = appPreferences.getValue(appPreferences.socketTimeout),
            )

        val serverId = appPreferences.getValue(appPreferences.currentServer) ?: return jellyfinApi

        // Temporary wrapped in runBlocking to support `database.getServerWithAddressAndUser`
        // suspend function.
        // TODO: move this logic somewhere else.
        runBlocking {
            val serverWithAddressAndUser =
                database.getServerWithAddressAndUser(serverId) ?: return@runBlocking jellyfinApi
            val serverAddress = serverWithAddressAndUser.address ?: return@runBlocking jellyfinApi
            val user = serverWithAddressAndUser.user

            jellyfinApi.apply {
                api.update(baseUrl = serverAddress.address, accessToken = user?.accessToken)
                userId = user?.id
            }
        }

        return jellyfinApi
    }
}
