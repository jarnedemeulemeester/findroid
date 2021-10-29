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
import java.util.*
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {
    @Singleton
    @Provides
    fun provideJellyfinApi(
        @ApplicationContext application: Context,
        sharedPreferences: SharedPreferences,
        serverDatabase: ServerDatabaseDao
    ): JellyfinApi {
        val jellyfinApi = JellyfinApi.getInstance(application, "")

        val serverId = sharedPreferences.getString("selectedServer", null)
        if (serverId != null) {
            val server = serverDatabase.get(serverId)
            jellyfinApi.apply {
                api.baseUrl = server.address
                api.accessToken = server.accessToken
                userId = UUID.fromString(server.userId)
            }
        }

        return jellyfinApi
    }
}