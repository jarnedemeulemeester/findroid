package dev.jdtech.jellyfin.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.ScreenManager
import androidx.car.app.model.Action
import androidx.car.app.model.CarIcon
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.repository.OfflinePackageRepository
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FindroidCarOfflineSeriesScreen(
    carContext: CarContext,
    private val series: FindroidCarCatalogItem,
    private val offlinePackageRepository: OfflinePackageRepository,
    private val jellyfinRepository: JellyfinRepository,
    private val jellyfinApi: JellyfinApi,
    private val appPreferences: AppPreferences,
) : Screen(carContext) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var loading = true
    private var seasons: List<FindroidCarCatalogItem> = emptyList()
    private var errorMessage: String? = null

    init {
        lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onCreate(owner: LifecycleOwner) {
                    loadSeasons()
                }

                override fun onDestroy(owner: LifecycleOwner) {
                    scope.cancel()
                }
            }
        )
    }

    override fun onGetTemplate(): Template {
        if (loading) {
            return MessageTemplate.Builder("Loading offline seasons")
                .setTitle(series.title)
                .setHeaderAction(Action.BACK)
                .setLoading(true)
                .build()
        }

        errorMessage?.let { message ->
            return MessageTemplate.Builder(message)
                .setTitle(series.title)
                .setHeaderAction(Action.BACK)
                .build()
        }

        val listBuilder = ItemList.Builder().setNoItemsMessage("No downloaded seasons")
        listBuilder.addItem(backRow())
        seasons.take(MAX_SEASON_ROWS).forEach { season ->
            val artwork = FindroidCarArtwork.iconFor(season.artworkPaths)
            listBuilder.addItem(
                Row.Builder()
                    .setTitle(season.title)
                    .apply {
                        artwork?.let { setImage(it, Row.IMAGE_TYPE_LARGE) }
                        if (season.subtitle.isNotBlank()) addText(season.subtitle)
                        season.secondaryStatusText().takeIf { it.isNotBlank() }?.let { addText(it) }
                    }
                    .setBrowsable(true)
                    .setOnClickListener {
                        carContext
                            .getCarService(ScreenManager::class.java)
                            .push(
                                FindroidCarOfflineSeasonScreen(
                                    carContext = carContext,
                                    series = series,
                                    season = season,
                                    offlinePackageRepository = offlinePackageRepository,
                                    jellyfinRepository = jellyfinRepository,
                                    jellyfinApi = jellyfinApi,
                                    appPreferences = appPreferences,
                                )
                            )
                    }
                    .build()
            )
        }

        return ListTemplate.Builder()
            .setTitle(series.title)
            .setHeaderAction(Action.BACK)
            .setSingleList(listBuilder.build())
            .build()
    }

    private fun loadSeasons() {
        scope.launch {
            loading = true
            errorMessage = null
            invalidate()

            runCatching {
                    withContext(Dispatchers.IO) {
                        loadOfflineCatalogItems().offlineSeasonItems(series)
                    }
                }
                .onSuccess { loadedSeasons ->
                    seasons = loadedSeasons
                    loading = false
                    errorMessage = null
                    invalidate()
                }
                .onFailure { throwable ->
                    seasons = emptyList()
                    loading = false
                    errorMessage = throwable.message ?: "Offline seasons unavailable"
                    invalidate()
                }
        }
    }

    private suspend fun loadOfflineCatalogItems(): List<FindroidCarCatalogItem> {
        val serverId = appPreferences.getValue(appPreferences.currentServer) ?: return emptyList()
        return offlinePackageRepository
            .getReadyItemSnapshotsByServerId(serverId)
            .map { snapshot ->
                snapshot.toFindroidCarCatalogItem(
                    offlinePackageRepository.getPackage(snapshot.packageId)
                )
            }
    }

    private fun backRow(): Row =
        Row.Builder()
            .setTitle("Back to offline series")
            .setImage(
                CarIcon.Builder(IconCompat.createWithResource(carContext, CoreR.drawable.ic_arrow_left))
                    .build(),
                Row.IMAGE_TYPE_ICON,
            )
            .setOnClickListener {
                carContext.getCarService(ScreenManager::class.java).pop()
            }
            .build()

    private fun FindroidCarCatalogItem.secondaryStatusText(): String =
        listOf(runtimeText, watchStatusText()).filter { it.isNotBlank() }.joinToString(" / ")

    private companion object {
        const val MAX_SEASON_ROWS = 100
    }
}
