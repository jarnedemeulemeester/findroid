package dev.jdtech.jellyfin.car

import android.net.Uri
import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.ScreenManager
import androidx.car.app.model.Action
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.models.FindroidSourceType
import dev.jdtech.jellyfin.repository.JellyfinRepository
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class FindroidCarItemScreen(
    carContext: CarContext,
    private val item: FindroidCarCatalogItem,
    private val jellyfinRepository: JellyfinRepository,
    private val jellyfinApi: JellyfinApi,
) : Screen(carContext) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var loadingPlayback = false
    private var playbackError: String? = null

    init {
        lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    scope.cancel()
                }
            }
        )
    }

    override fun onGetTemplate(): Template {
        if (loadingPlayback) {
            return MessageTemplate.Builder("Preparing online stream")
                .setTitle(item.title)
                .setHeaderAction(Action.BACK)
                .setLoading(true)
                .build()
        }

        playbackError?.let { message ->
            return MessageTemplate.Builder(message)
                .setTitle(item.title)
                .setHeaderAction(Action.BACK)
                .build()
        }

        val artwork = FindroidCarArtwork.iconFor(item.artworkPaths)
        val rows =
            ItemList.Builder()
                .addItem(
                    Row.Builder()
                        .setTitle(
                            if (item.itemKind == FindroidCarCatalogItemKind.SERIES) "Episodes"
                            else "Play"
                        )
                        .addText(
                            when {
                                item.itemKind == FindroidCarCatalogItemKind.SERIES ->
                                    "Browse Jellyfin episodes"
                                item.videoPath != null -> "Findroid local video"
                                else -> "Jellyfin online stream"
                            }
                        )
                        .apply { artwork?.let { setImage(it, Row.IMAGE_TYPE_LARGE) } }
                        .setOnClickListener { play() }
                        .build()
                )
                .addItem(
                    Row.Builder()
                        .setTitle("Source")
                        .addText(
                            item.subtitle.ifBlank {
                                if (item.videoPath != null) "Offline video" else "Online library"
                            }
                        )
                        .addText(
                            item.runtimeText.ifBlank {
                                if (item.videoPath != null) "Ready" else "Available"
                            }
                        )
                        .build()
                )
                .build()

        return ListTemplate.Builder()
            .setTitle(item.title)
            .setHeaderAction(Action.BACK)
            .setSingleList(rows)
            .build()
    }

    private fun play() {
        if (!item.videoPath.isNullOrBlank() || !item.streamUrl.isNullOrBlank()) {
            pushVideo(item)
            return
        }

        if (item.itemKind == FindroidCarCatalogItemKind.SERIES) {
            pushSeriesEpisodes()
            return
        }

        resolveOnlineStreamAndPlay()
    }

    private fun resolveOnlineStreamAndPlay() {
        if (loadingPlayback) return
        loadingPlayback = true
        playbackError = null
        invalidate()

        scope.launch {
            runCatching {
                    withContext(Dispatchers.IO) {
                        val itemId = UUID.fromString(item.itemId)
                        val sources = jellyfinRepository.getMediaSources(itemId, includePath = true)
                        val source = sources.findroidStandardPlaybackSource()
                            ?: error("No playable online source")
                        Timber.i(
                            "FindroidCarItemScreen resolved playback source using standard Findroid selection sourceId=%s sourceType=%s path=%s",
                            source.id,
                            source.type,
                            Uri.parse(source.path).toSanitizedLogString(),
                        )
                        Log.i(
                            FINDROID_CAR_AA_LOG_TAG,
                            "FindroidCarItemScreen resolved playback source using standard Findroid selection sourceType=${source.type}",
                        )
                        source
                    }
                }
                .onSuccess { source ->
                    loadingPlayback = false
                    pushVideo(
                        item.copy(
                            videoPath =
                                source.path.takeIf {
                                    source.type == FindroidSourceType.LOCAL
                                },
                            streamUrl =
                                source.path.takeIf {
                                    source.type != FindroidSourceType.LOCAL
                                },
                        )
                    )
                }
                .onFailure { throwable ->
                    loadingPlayback = false
                    playbackError = throwable.message ?: "Online playback failed"
                    invalidate()
                }
        }
    }

    private fun pushVideo(playableItem: FindroidCarCatalogItem) {
        carContext
            .getCarService(ScreenManager::class.java)
            .push(FindroidCarVideoScreen(carContext, playableItem, jellyfinRepository))
    }

    private fun pushSeriesEpisodes() {
        carContext
            .getCarService(ScreenManager::class.java)
            .push(
                FindroidCarSeriesScreen(
                    carContext = carContext,
                    series = item,
                    jellyfinRepository = jellyfinRepository,
                    jellyfinApi = jellyfinApi,
                )
            )
    }

    private fun Uri.toSanitizedLogString(): String =
        buildUpon().clearQuery().fragment(null).build().toString()

    private companion object {
        const val FINDROID_CAR_AA_LOG_TAG = "FindroidCarAA"
    }
}
