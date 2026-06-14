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
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.repository.JellyfinRepository
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.model.api.BaseItemKind
import timber.log.Timber

class FindroidCarSeasonScreen(
    carContext: CarContext,
    private val series: FindroidCarCatalogItem,
    private val season: FindroidCarCatalogItem,
    private val jellyfinRepository: JellyfinRepository,
    private val jellyfinApi: JellyfinApi,
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
            return MessageTemplate.Builder("Loading episodes")
                .setTitle(season.title)
                .setHeaderAction(Action.BACK)
                .setLoading(true)
                .build()
        }

        val listBuilder = ItemList.Builder().setNoItemsMessage("No episodes")
        listBuilder.addItem(backRow())
        val statusMessage =
            errorMessage ?: if (episodes.isEmpty()) "No episodes returned for this season" else null
        if (statusMessage != null) {
            listBuilder.addItem(statusRow(statusMessage))
        } else {
            episodes.take(MAX_EPISODE_ROWS).forEach { episode ->
                val artwork = FindroidCarArtwork.iconFor(episode.artworkPaths)
                listBuilder.addItem(
                    Row.Builder()
                        .setTitle(episode.title)
                        .apply {
                            artwork?.let { setImage(it, Row.IMAGE_TYPE_LARGE) }
                            if (episode.runtimeText.isNotBlank()) addText(episode.runtimeText)
                            episode.watchStatusText().takeIf { it.isNotBlank() }?.let { addText(it) }
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
                        val seriesId = UUID.fromString(series.itemId)
                        val seasonId = UUID.fromString(season.itemId)
                        loadEpisodeCatalogItems(seriesId, seasonId)
                    }
                }
                .onSuccess { loadedEpisodes ->
                    Timber.i(
                        "FindroidCarSeasonScreen episodes loaded seriesId=%s seasonId=%s seriesTitle=%s seasonTitle=%s count=%s",
                        series.itemId,
                        season.itemId,
                        series.title,
                        season.title,
                        loadedEpisodes.size,
                    )
                    episodes = loadedEpisodes
                    loading = false
                    errorMessage = null
                    invalidate()
                }
                .onFailure { throwable ->
                    Timber.w(
                        "FindroidCarSeasonScreen episodes load failed seriesId=%s seasonId=%s seriesTitle=%s seasonTitle=%s errorType=%s",
                        series.itemId,
                        season.itemId,
                        series.title,
                        season.title,
                        throwable.javaClass.simpleName,
                    )
                    episodes = emptyList()
                    loading = false
                    errorMessage = "Episodes unavailable"
                    invalidate()
                }
        }
    }

    private suspend fun loadEpisodeCatalogItems(
        seriesId: UUID,
        seasonId: UUID,
    ): List<FindroidCarCatalogItem> {
        val seasonIndex = season.parentIndexNumber
        val primaryEpisodes = jellyfinRepository.getEpisodes(seriesId, seasonId)
        Timber.i(
            "FindroidCarSeasonScreen primary getEpisodes seriesId=%s seasonId=%s seasonIndex=%s seasonTitle=%s count=%s",
            seriesId,
            seasonId,
            seasonIndex,
            season.title,
            primaryEpisodes.size,
        )
        if (primaryEpisodes.isNotEmpty()) return primaryEpisodes.toCatalogItems()

        val seriesEpisodes =
            jellyfinRepository
                .getItems(
                    parentId = seriesId,
                    includeTypes = listOf(BaseItemKind.EPISODE),
                    recursive = true,
                )
                .filterIsInstance<FindroidEpisode>()
        val matchingSeriesEpisodes =
            seriesEpisodes.filter { episode ->
                episode.belongsToOnlineSeason(seriesId, seasonId, seasonIndex)
            }
        Timber.w(
            "FindroidCarSeasonScreen fallback series episode scan seriesId=%s seasonId=%s seasonIndex=%s seasonTitle=%s allCount=%s matchedCount=%s",
            seriesId,
            seasonId,
            seasonIndex,
            season.title,
            seriesEpisodes.size,
            matchingSeriesEpisodes.size,
        )
        if (matchingSeriesEpisodes.isNotEmpty()) return matchingSeriesEpisodes.toCatalogItems()

        val seasonChildEpisodes =
            jellyfinRepository
                .getItems(
                    parentId = seasonId,
                    includeTypes = listOf(BaseItemKind.EPISODE),
                    recursive = true,
                )
                .filterIsInstance<FindroidEpisode>()
        Timber.w(
            "FindroidCarSeasonScreen fallback season child scan seriesId=%s seasonId=%s seasonIndex=%s seasonTitle=%s count=%s",
            seriesId,
            seasonId,
            seasonIndex,
            season.title,
            seasonChildEpisodes.size,
        )
        return seasonChildEpisodes.toCatalogItems()
    }

    private fun List<FindroidEpisode>.toCatalogItems(): List<FindroidCarCatalogItem> =
        sortedWith(compareBy<FindroidEpisode> { it.indexNumber }.thenBy { it.name })
            .mapNotNull { episode ->
                episode.toFindroidCarCatalogItemWithCachedArtwork(
                    carContext.filesDir,
                    jellyfinApi.api.accessToken,
                ) ?: episode.toFindroidCarCatalogItem(carContext.filesDir)
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

    private fun statusRow(message: String): Row = Row.Builder().setTitle(message).build()

    private companion object {
        const val MAX_EPISODE_ROWS = 100
    }
}
