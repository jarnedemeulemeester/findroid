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
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FindroidCarHistoryScreen(
    carContext: CarContext,
    private val offlinePackageRepository: OfflinePackageRepository,
    private val jellyfinRepository: JellyfinRepository,
    private val jellyfinApi: JellyfinApi,
    private val appPreferences: AppPreferences,
) : Screen(carContext) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var loading = true
    private var items: List<FindroidCarCatalogItem> = emptyList()
    private var errorMessage: String? = null

    init {
        lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onCreate(owner: LifecycleOwner) {
                    loadHistory()
                }

                override fun onDestroy(owner: LifecycleOwner) {
                    scope.cancel()
                }
            }
        )
    }

    override fun onGetTemplate(): Template {
        if (loading) {
            return MessageTemplate.Builder("Loading history")
                .setTitle("Continue watching")
                .setHeaderAction(Action.BACK)
                .setLoading(true)
                .build()
        }

        errorMessage?.let { message ->
            return MessageTemplate.Builder(message)
                .setTitle("Continue watching")
                .setHeaderAction(Action.BACK)
                .build()
        }

        val list = ItemList.Builder().setNoItemsMessage("No watched items yet")
        list.addItem(backRow())
        items.take(MAX_HISTORY_ITEMS).forEach { item ->
            val artwork = FindroidCarArtwork.iconFor(item.artworkPaths)
            list.addItem(
                Row.Builder()
                    .setTitle(item.title)
                    .apply {
                        artwork?.let { setImage(it, Row.IMAGE_TYPE_LARGE) }
                        if (item.subtitle.isNotBlank()) addText(item.subtitle)
                        item.resumeStatusText().takeIf { it.isNotBlank() }?.let { addText(it) }
                    }
                    .setBrowsable(true)
                    .setOnClickListener { openItem(item) }
                    .build()
            )
        }

        return ListTemplate.Builder()
            .setTitle("Continue watching")
            .setHeaderAction(Action.BACK)
            .setSingleList(list.build())
            .build()
    }

    private fun loadHistory() {
        scope.launch {
            loading = true
            errorMessage = null
            invalidate()

            runCatching {
                    withContext(Dispatchers.IO) {
                        val serverId = appPreferences.getValue(appPreferences.currentServer)
                        val offlineItems =
                            serverId
                                ?.let { offlinePackageRepository.getReadyItemSnapshotsByServerId(it) }
                                .orEmpty()
                                .map { snapshot ->
                                    snapshot.toFindroidCarCatalogItem(
                                        offlinePackageRepository.getPackage(snapshot.packageId)
                                    )
                                }
                                .filter { !it.videoPath.isNullOrBlank() }

                        val onlineResumeItems =
                            runCatching { jellyfinRepository.getResumeItems() }.getOrDefault(emptyList())

                        val serverResumeItems =
                            onlineResumeItems.mapNotNull { item ->
                                item.toFindroidCarCatalogItemWithCachedArtwork(
                                    carContext.filesDir,
                                    jellyfinApi.api.accessToken,
                                ) ?: item.toFindroidCarCatalogItem(carContext.filesDir)
                            }
                        val historyEntries = FindroidCarPlaybackHistory.loadEntries(carContext)
                        val userDataByItemId =
                            loadUserDataOverlays(serverResumeItems, offlineItems, historyEntries)

                        FindroidCarContinueWatchingResolver.resolve(
                            serverResumeItems = serverResumeItems,
                            offlineItems = offlineItems,
                            historyEntries = historyEntries,
                            userDataByItemId = userDataByItemId,
                            maxItems = MAX_HISTORY_ITEMS,
                        )
                    }
                }
                .onSuccess { loadedItems ->
                    items = loadedItems
                    loading = false
                    errorMessage = null
                    invalidate()
                }
                .onFailure { throwable ->
                    items = emptyList()
                    loading = false
                    errorMessage = throwable.message ?: "History unavailable"
                    invalidate()
                }
        }
    }

    private fun openItem(item: FindroidCarCatalogItem) {
        carContext
            .getCarService(ScreenManager::class.java)
            .push(
                FindroidCarItemScreen(
                    carContext = carContext,
                    item = item,
                    jellyfinRepository = jellyfinRepository,
                    jellyfinApi = jellyfinApi,
                )
            )
    }

    private fun backRow(): Row =
        Row.Builder()
            .setTitle("Back to library")
            .setImage(
                CarIcon.Builder(IconCompat.createWithResource(carContext, CoreR.drawable.ic_arrow_left))
                    .build(),
                Row.IMAGE_TYPE_ICON,
            )
            .setOnClickListener { carContext.getCarService(ScreenManager::class.java).pop() }
            .build()

    private fun FindroidCarCatalogItem.resumeStatusText(): String =
        when {
            playbackPositionTicks > 0L -> "Resume from ${formatTicks(playbackPositionTicks)}"
            else -> watchStatusText()
        }

    private suspend fun loadUserDataOverlays(
        serverResumeItems: List<FindroidCarCatalogItem>,
        offlineItems: List<FindroidCarCatalogItem>,
        historyEntries: List<FindroidCarPlaybackHistory.Entry>,
    ): Map<String, FindroidCarUserDataOverlay> =
        (serverResumeItems.map { it.itemId } +
                offlineItems.map { it.itemId } +
                historyEntries.map { it.item.itemId })
            .distinct()
            .mapNotNull { itemId ->
                val uuid = itemId.uuidOrNull() ?: return@mapNotNull null
                val userData =
                    runCatching { jellyfinRepository.getUserData(uuid) }.getOrNull()
                        ?: return@mapNotNull null
                itemId to
                    FindroidCarUserDataOverlay(
                        played = userData.played,
                        playbackPositionTicks = userData.playbackPositionTicks,
                        toBeSynced = userData.toBeSynced,
                    )
            }
            .toMap()

    private fun formatTicks(ticks: Long): String {
        val totalSeconds = ticks / TICKS_PER_SECOND
        val seconds = totalSeconds % 60
        val minutes = (totalSeconds / 60) % 60
        val hours = totalSeconds / 3600
        return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
        else "%02d:%02d".format(minutes, seconds)
    }

    private fun String.uuidOrNull(): UUID? = runCatching { UUID.fromString(this) }.getOrNull()

    private companion object {
        const val MAX_HISTORY_ITEMS = 10
        const val TICKS_PER_SECOND = 10_000_000L
    }
}
