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
import androidx.car.app.model.Tab
import androidx.car.app.model.TabContents
import androidx.car.app.model.TabTemplate
import androidx.car.app.model.Template
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.SortBy
import dev.jdtech.jellyfin.models.SortOrder
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.repository.OfflinePackageRepository
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.model.api.BaseItemKind

class FindroidCarLibraryScreen(
    carContext: CarContext,
    private val surface: FindroidCarSurface,
    private val source: FindroidCarLibrarySource,
    private val offlinePackageRepository: OfflinePackageRepository,
    private val jellyfinRepository: JellyfinRepository,
    private val jellyfinApi: JellyfinApi,
    private val appPreferences: AppPreferences,
) : Screen(carContext) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var activeTabId =
        when (source) {
            FindroidCarLibrarySource.OFFLINE -> TAB_SERIES
            FindroidCarLibrarySource.ONLINE -> TAB_MOVIES
        }
    private var loading = true
    private var items: List<FindroidCarCatalogItem> = emptyList()
    private var errorMessage: String? = null

    init {
        lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onCreate(owner: LifecycleOwner) {
                    loadItems()
                }

                override fun onDestroy(owner: LifecycleOwner) {
                    scope.cancel()
                }
            }
        )
    }

    override fun onGetTemplate(): Template {
        if (loading) {
            return MessageTemplate.Builder("Loading ${source.title} library")
                .setTitle(surface.title)
                .setHeaderAction(Action.BACK)
                .setLoading(true)
                .build()
        }

        errorMessage?.let { message ->
            return MessageTemplate.Builder(message)
                .setTitle("${source.title} library")
                .setHeaderAction(Action.BACK)
                .build()
        }

        val content = buildListTemplate(items)
        return TabTemplate.Builder(
                object : TabTemplate.TabCallback {
                    override fun onTabSelected(tabContentId: String) {
                        if (activeTabId == tabContentId) return
                        activeTabId = tabContentId
                        loadItems()
                    }
                }
            )
            .setHeaderAction(Action.APP_ICON)
            .addTab(tab(TAB_MOVIES, "Movies", CoreR.drawable.ic_film))
            .addTab(tab(TAB_SERIES, "Series", CoreR.drawable.ic_tv))
            .setActiveTabContentId(activeTabId)
            .setTabContents(TabContents.Builder(content).build())
            .build()
    }

    private fun loadItems() {
        scope.launch {
            loading = true
            errorMessage = null
            invalidate()

            runCatching {
                    withContext(Dispatchers.IO) {
                        when (source) {
                            FindroidCarLibrarySource.OFFLINE -> loadOfflineItems(activeTabId)
                            FindroidCarLibrarySource.ONLINE -> loadOnlineItems(activeTabId)
                        }
                    }
                }
                .onSuccess { loadedItems ->
                    items = loadedItems.sortedWith(carItemComparator)
                    loading = false
                    errorMessage = null
                    invalidate()
                }
                .onFailure { throwable ->
                    items = emptyList()
                    loading = false
                    errorMessage = errorMessage(throwable)
                    invalidate()
                }
        }
    }

    private suspend fun loadOfflineItems(tabId: String): List<FindroidCarCatalogItem> {
        val serverId =
            appPreferences.getValue(appPreferences.currentServer) ?: return emptyList()
        val catalogItems =
            offlinePackageRepository
            .getReadyItemSnapshotsByServerId(serverId)
            .map { snapshot ->
                snapshot.toFindroidCarCatalogItem(
                    offlinePackageRepository.getPackage(snapshot.packageId)
                )
            }
        return when (tabId) {
            TAB_MOVIES -> catalogItems.filter { it.itemKind == FindroidCarCatalogItemKind.MOVIE }
            TAB_SERIES -> catalogItems.offlineSeriesItems()
            else -> emptyList()
            }
    }

    private suspend fun loadOnlineItems(tabId: String): List<FindroidCarCatalogItem> {
        requireOnlineSession()
        val itemType =
            when (tabId) {
                TAB_MOVIES -> BaseItemKind.MOVIE
                TAB_SERIES -> BaseItemKind.SERIES
                else -> return emptyList()
            }
        return jellyfinRepository
            .getItems(
                includeTypes = listOf(itemType),
                recursive = true,
                sortBy = SortBy.NAME,
                sortOrder = SortOrder.ASCENDING,
                limit = MAX_ROWS,
            )
            .mapNotNull { item ->
                val catalogItem = item.toFindroidCarCatalogItem(carContext.filesDir)
                catalogItem?.let { item to it }
            }
            .sortedWith(
                compareBy<Pair<FindroidItem, FindroidCarCatalogItem>> { it.second.itemKind.ordinal }
                    .thenBy { it.second.subtitle }
                    .thenBy { it.second.title }
            )
            .mapIndexed { index, (item, catalogItem) ->
                when {
                    index < ARTWORK_PREFETCH_ROWS ->
                        item.toFindroidCarCatalogItemWithCachedArtwork(
                            carContext.filesDir,
                            jellyfinApi.api.accessToken,
                        ) ?: catalogItem
                    else -> catalogItem
                }
            }
    }

    private fun requireOnlineSession() {
        val hasServer = appPreferences.getValue(appPreferences.currentServer) != null
        val hasUser = jellyfinApi.userId != null
        val hasToken = !jellyfinApi.api.accessToken.isNullOrBlank()
        check(hasServer && hasUser && hasToken) { SIGN_IN_REQUIRED_MESSAGE }
    }

    private fun buildListTemplate(tabItems: List<FindroidCarCatalogItem>): ListTemplate {
        val listBuilder = ItemList.Builder().setNoItemsMessage(noItemsMessage())
        listBuilder.addItem(backRow())
        tabItems.take(MAX_ROWS).forEach { item ->
            val artwork = FindroidCarArtwork.iconFor(item.artworkPaths)
            listBuilder.addItem(
                Row.Builder()
                    .setTitle(item.title)
                    .apply {
                        artwork?.let { setImage(it, Row.IMAGE_TYPE_LARGE) }
                        if (item.subtitle.isNotBlank()) addText(item.subtitle)
                        item.secondaryStatusText().takeIf { it.isNotBlank() }?.let { addText(it) }
                    }
                    .setBrowsable(true)
                    .setOnClickListener {
                        openItem(item)
                    }
                    .build()
            )
        }
        return ListTemplate.Builder().setSingleList(listBuilder.build()).build()
    }

    private fun openItem(item: FindroidCarCatalogItem) {
        val screenManager = carContext.getCarService(ScreenManager::class.java)
        if (source == FindroidCarLibrarySource.ONLINE && item.itemKind == FindroidCarCatalogItemKind.SERIES) {
            screenManager.push(
                FindroidCarSeriesScreen(
                    carContext = carContext,
                    series = item,
                    jellyfinRepository = jellyfinRepository,
                    jellyfinApi = jellyfinApi,
                )
            )
            return
        }

        if (source == FindroidCarLibrarySource.OFFLINE && item.itemKind == FindroidCarCatalogItemKind.SERIES) {
            screenManager.push(
                FindroidCarOfflineSeriesScreen(
                    carContext = carContext,
                    series = item,
                    offlinePackageRepository = offlinePackageRepository,
                    jellyfinRepository = jellyfinRepository,
                    jellyfinApi = jellyfinApi,
                    appPreferences = appPreferences,
                )
            )
            return
        }

        screenManager.push(
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
            .setOnClickListener {
                carContext.getCarService(ScreenManager::class.java).pop()
            }
            .build()

    private fun noItemsMessage(): String =
        when (activeTabId) {
            TAB_MOVIES -> "No ${source.title.lowercase()} movies"
            TAB_SERIES -> "No ${source.title.lowercase()} series"
            else -> "No items"
        }

    private fun errorMessage(throwable: Throwable): String =
        when {
            throwable.message == SIGN_IN_REQUIRED_MESSAGE -> SIGN_IN_REQUIRED_MESSAGE
            throwable is NullPointerException ->
                "${source.title} library unavailable. Open Findroid on the phone and sign in."
            else -> throwable.message ?: "${source.title} library unavailable"
        }

    private fun tab(contentId: String, title: String, iconRes: Int): Tab =
        Tab.Builder()
            .setContentId(contentId)
            .setTitle(title)
            .setIcon(
                CarIcon.Builder(IconCompat.createWithResource(carContext, iconRes)).build()
            )
            .build()

    private fun FindroidCarCatalogItem.secondaryStatusText(): String =
        listOf(runtimeText, watchStatusText()).filter { it.isNotBlank() }.joinToString(" / ")

    private companion object {
        const val TAB_MOVIES = "movies"
        const val TAB_SERIES = "series"
        const val MAX_ROWS = 100
        const val ARTWORK_PREFETCH_ROWS = 20
        const val SIGN_IN_REQUIRED_MESSAGE = "Open Findroid on the phone and sign in first."

        val carItemComparator =
            compareBy<FindroidCarCatalogItem>(
                { it.itemKind.ordinal },
                { it.subtitle },
                { it.title },
            )
    }
}
