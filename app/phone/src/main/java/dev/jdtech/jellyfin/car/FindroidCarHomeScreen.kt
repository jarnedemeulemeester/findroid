package dev.jdtech.jellyfin.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.ScreenManager
import androidx.car.app.model.Action
import androidx.car.app.model.CarIcon
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.core.graphics.drawable.IconCompat
import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.repository.OfflinePackageRepository
import dev.jdtech.jellyfin.settings.domain.AppPreferences

class FindroidCarHomeScreen(
    carContext: CarContext,
    private val surface: FindroidCarSurface,
    private val offlinePackageRepository: OfflinePackageRepository,
    private val jellyfinRepository: JellyfinRepository,
    private val jellyfinApi: JellyfinApi,
    private val appPreferences: AppPreferences,
) : Screen(carContext) {

    override fun onGetTemplate(): Template {
        val rows =
            ItemList.Builder()
                .addItem(historyRow())
                .addItem(sourceRow(FindroidCarLibrarySource.OFFLINE))
                .addItem(sourceRow(FindroidCarLibrarySource.ONLINE))
                .build()

        return ListTemplate.Builder()
            .setTitle("${surface.title} Library")
            .setHeaderAction(Action.APP_ICON)
            .setSingleList(rows)
            .build()
    }

    private fun historyRow(): Row =
        Row.Builder()
            .setTitle("Continue watching")
            .addText("Resume the last 10 movies and episodes")
            .setImage(
                CarIcon.Builder(IconCompat.createWithResource(carContext, CoreR.drawable.ic_play))
                    .build(),
                Row.IMAGE_TYPE_ICON,
            )
            .setBrowsable(true)
            .setOnClickListener {
                carContext
                    .getCarService(ScreenManager::class.java)
                    .push(
                        FindroidCarHistoryScreen(
                            carContext = carContext,
                            offlinePackageRepository = offlinePackageRepository,
                            jellyfinRepository = jellyfinRepository,
                            jellyfinApi = jellyfinApi,
                            appPreferences = appPreferences,
                        )
                    )
            }
            .build()

    private fun sourceRow(source: FindroidCarLibrarySource): Row =
        Row.Builder()
            .setTitle(source.title)
            .addText(source.description)
            .setImage(
                CarIcon.Builder(IconCompat.createWithResource(carContext, source.iconRes)).build(),
                Row.IMAGE_TYPE_ICON,
            )
            .setBrowsable(true)
            .setOnClickListener {
                carContext
                    .getCarService(ScreenManager::class.java)
                    .push(
                        FindroidCarLibraryScreen(
                            carContext = carContext,
                            surface = surface,
                            source = source,
                            offlinePackageRepository = offlinePackageRepository,
                            jellyfinRepository = jellyfinRepository,
                            jellyfinApi = jellyfinApi,
                            appPreferences = appPreferences,
                        )
                    )
            }
            .build()
}

enum class FindroidCarLibrarySource(
    val title: String,
    val description: String,
    val iconRes: Int,
) {
    OFFLINE(
        title = "Offline",
        description = "Downloaded Movies/Findroid videos",
        iconRes = CoreR.drawable.ic_download,
    ),
    ONLINE(
        title = "Online",
        description = "Browse Jellyfin movies and series",
        iconRes = CoreR.drawable.ic_globe,
    ),
}
