package dev.jdtech.jellyfin.setup.di

import android.Manifest
import android.os.Build
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.jdtech.jellyfin.settings.R as SettingsR
import dev.jdtech.jellyfin.settings.domain.AppPreferences
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
            permission = Manifest.permission.ACCESS_LOCAL_NETWORK,
            applicableSince = Build.VERSION_CODES.CINNAMON_BUN,
            title = SetupR.string.permission_local_network_title,
            rationale = SetupR.string.permission_local_network_rationale,
            icon = SettingsR.drawable.ic_network,
            decidedPreference = appPreferences.localNetworkPermissionDecided,
        )
}
