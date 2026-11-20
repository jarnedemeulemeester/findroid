package dev.jdtech.jellyfin.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.core.domain.ServerAddressSelector
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {
    @Singleton
    @Provides
    fun provideJellyfinApi(
        @ApplicationContext application: Context,
        appPreferences: AppPreferences,
        database: ServerDatabaseDao,
        serverAddressSelector: ServerAddressSelector,
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
        runBlocking(Dispatchers.IO) {
            // Switch to the fastest address of the current server on every app start. Bounded in
            // time so an unreachable address can not delay the start of the app for too long.
            if (appPreferences.getValue(appPreferences.autoSelectFastestServerAddress)) {
                withTimeoutOrNull(ADDRESS_SELECTION_TIMEOUT_MS) {
                    serverAddressSelector.selectFastestAddress(serverId)
                }
            }

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

    private const val ADDRESS_SELECTION_TIMEOUT_MS = 2_000L
}
