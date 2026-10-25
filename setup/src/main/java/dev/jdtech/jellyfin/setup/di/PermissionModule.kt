package dev.jdtech.jellyfin.setup.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import dev.jdtech.jellyfin.settings.domain.models.PermissionDefinitions
import dev.jdtech.jellyfin.setup.presentation.permission.PermissionRequest
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PermissionModule {
    /** The permissions that gate app entry, in the order they are asked. */
    @Provides
    @Singleton
    fun providePermissionRequests(appPreferences: AppPreferences): List<PermissionRequest> =
        listOf(
            PermissionRequest(
                permission = PermissionDefinitions.localNetwork,
                decidedPreference = appPreferences.localNetworkPermissionDecided,
            )
        )
}
