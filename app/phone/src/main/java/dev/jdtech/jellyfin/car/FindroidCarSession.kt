package dev.jdtech.jellyfin.car

import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.Session
import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.repository.OfflinePackageRepository
import dev.jdtech.jellyfin.settings.domain.AppPreferences

class FindroidCarSession(
    private val surface: FindroidCarSurface,
    private val offlinePackageRepository: OfflinePackageRepository,
    private val jellyfinRepository: JellyfinRepository,
    private val jellyfinApi: JellyfinApi,
    private val appPreferences: AppPreferences,
) : Session() {
    override fun onCreateScreen(intent: Intent): Screen =
        FindroidCarHomeScreen(
            carContext = carContext,
            surface = surface,
            offlinePackageRepository = offlinePackageRepository,
            jellyfinRepository = jellyfinRepository,
            jellyfinApi = jellyfinApi,
            appPreferences = appPreferences,
        )
}
