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

class FindroidCarOfflineSeasonScreen(
    carContext: CarContext,
    private val series: FindroidCarCatalogItem,
    private val season: FindroidCarCatalogItem,
    private val offlinePackageRepository: OfflinePackageRepository,
    private val jellyfinRepository: JellyfinRepository,
    private val jellyfinApi: JellyfinApi,
    private val appPreferences: AppPreferences,
) : Screen(carContext) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var loading = true
    private var episodes: List<FindroidCarCatalogItem> = emptyList()
    private var errorMessage: String? = null

    init {
        lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onCreate(owner: LifecycleOwner) {
                    loadEpisodes()
                }

                override fun onDestroy(owner: LifecycleOwner) {
                    scope.cancel()
                }
            }
        )
    }

    override fun onGetTemplate(): Template {
        if (loading) {
            return MessageTemplate.Builder("Loading offline episodes")
                .setTitle(season.title)
                .setHeaderAction(Action.BACK)
                .setLoading(true)
                .build()
        }

        errorMessage?.let { message ->
            return MessageTemplate.Builder(message)
                .setTitle(season.title)
                .setHeaderAction(Action.BACK)
                .build()
        }

        val listBuilder = ItemList.Builder().setNoItemsMessage("No downloaded episodes")
        listBuilder.addItem(backRow())
        episodes.take(MAX_EPISODE_ROWS).forEach { episode ->
            val artwork = FindroidCarArtwork.iconFor(episode.artworkPaths)
            listBuilder.addItem(
                Row.Builder()
                    .setTitle(episode.title)
                    .apply {
                        artwork?.let { setImage(it, Row.IMAGE_TYPE_LARGE) }
                        episode.secondaryStatusText().takeIf { it.isNotBlank() }?.let { addText(it) }
                    }
                    .setBrowsable(true)
                    .setOnClickListener {
                        carContext
                            .getCarService(ScreenManager::class.java)
                            .push(
                                FindroidCarItemScreen(
                                    carContext = carContext,
                                    item = episode,
                                    jellyfinRepository = jellyfinRepository,
                                    jellyfinApi = jellyfinApi,
                                )
                            )
                    }
                    .build()
            )
        }

        return ListTemplate.Builder()
            .setTitle("${series.title} / ${season.title}")
            .setHeaderAction(Action.BACK)
            .setSingleList(listBuilder.build())
            .build()
    }

    private fun loadEpisodes() {
        scope.launch {
            loading = true
            errorMessage = null
            invalidate()

            runCatching {
                    withContext(Dispatchers.IO) {
                        loadOfflineCatalogItems().offlineEpisodeItems(series, season)
                    }
                }
                .onSuccess { loadedEpisodes ->
                    episodes = loadedEpisodes
                    loading = false
                    errorMessage = null
                    invalidate()
                }
                .onFailure { throwable ->
                    episodes = emptyList()
                    loading = false
                    errorMessage = throwable.message ?: "Offline episodes unavailable"
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
            .setTitle("Back to seasons")
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
        const val MAX_EPISODE_ROWS = 100
    }
}
