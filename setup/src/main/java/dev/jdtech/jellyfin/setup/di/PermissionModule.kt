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
    @Provides
    @Singleton
    fun providePermissionRequest(appPreferences: AppPreferences): PermissionRequest =
        PermissionRequest(
            permission = PermissionDefinitions.localNetwork,
            decidedPreference = appPreferences.localNetworkPermissionDecided,
        )
}
