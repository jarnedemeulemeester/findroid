package dev.jdtech.jellyfin.car

import android.content.pm.ApplicationInfo
import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.repository.OfflinePackageRepository
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import javax.inject.Inject

@AndroidEntryPoint
class FindroidMediaCarAppService : CarAppService() {
    @Inject lateinit var offlinePackageRepository: OfflinePackageRepository
    @Inject lateinit var jellyfinRepository: JellyfinRepository
    @Inject lateinit var jellyfinApi: JellyfinApi
    @Inject lateinit var appPreferences: AppPreferences

    override fun createHostValidator(): HostValidator =
        createFindroidHostValidator(this, applicationInfo)

    override fun onCreateSession(): Session =
        FindroidCarSession(
            surface = FindroidCarSurface.MEDIA,
            offlinePackageRepository = offlinePackageRepository,
            jellyfinRepository = jellyfinRepository,
            jellyfinApi = jellyfinApi,
            appPreferences = appPreferences,
        )
}

internal fun createFindroidHostValidator(
    service: CarAppService,
    applicationInfo: ApplicationInfo,
): HostValidator =
    if ((applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
        HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    } else {
        HostValidator.Builder(service)
            .addAllowedHosts(androidx.car.app.R.array.hosts_allowlist_sample)
            .build()
    }
