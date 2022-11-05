package dev.jdtech.jellyfin.di

import android.content.Context
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.utils.AppPreferences
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {
    @Singleton
    @Provides
    fun provideJellyfinApi(
        @ApplicationContext application: Context,
        sharedPreferences: SharedPreferences,
        appPreferences: AppPreferences,
        serverDatabase: ServerDatabaseDao
    ): JellyfinApi {
        val jellyfinApi = JellyfinApi.getInstance(
            context = application,
            requestTimeout = appPreferences.requestTimeout,
            connectTimeout = appPreferences.connectTimeout,
            socketTimeout = appPreferences.socketTimeout
        )

        val serverId = sharedPreferences.getString("selectedServer", null)
        if (serverId != null) {
            val serverWithAddressesAndUsers = serverDatabase.getServerWithAddressesAndUsers(serverId) ?: return jellyfinApi
            val server = serverWithAddressesAndUsers.server
            val serverAddress = serverWithAddressesAndUsers.addresses.firstOrNull { it.id == server.currentServerAddressId } ?: return jellyfinApi
            val user = serverWithAddressesAndUsers.users.firstOrNull { it.id == server.currentUserId } ?: return jellyfinApi
            jellyfinApi.apply {
                api.baseUrl = serverAddress.address
                api.accessToken = user.accessToken
                userId = user.id
            }
        }

        return jellyfinApi
    }
}
