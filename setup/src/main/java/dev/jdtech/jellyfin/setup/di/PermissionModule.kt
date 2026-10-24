package dev.jdtech.jellyfin.setup.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import dev.jdtech.jellyfin.settings.domain.models.PermissionDefinitions
import dev.jdtech.jellyfin.setup.R as SetupR
import dev.jdtech.jellyfin.setup.presentation.permission.PermissionRequest
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PermissionModule {
    /**
     * Provides the permission that gates app entry: it gets a rationale screen in the setup flow
     * and an interstitial before the home screen for existing users. When a second permission is
     * added (e.g. POST_NOTIFICATIONS), introduce a qualifier for the additional binding.
     */
    @Provides
    @Singleton
    fun providePermissionRequest(appPreferences: AppPreferences): PermissionRequest =
        PermissionRequest(
            permission = PermissionDefinitions.localNetwork,
            title = SetupR.string.permission_local_network_title,
            rationale = SetupR.string.permission_local_network_rationale,
            decidedPreference = appPreferences.localNetworkPermissionDecided,
        )
}
