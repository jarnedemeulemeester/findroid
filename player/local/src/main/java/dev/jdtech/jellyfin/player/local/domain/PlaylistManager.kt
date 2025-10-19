package dev.jdtech.jellyfin.player.local.domain

import androidx.core.net.toUri
import androidx.media3.common.MimeTypes
import dev.jdtech.jellyfin.models.JellyCastChapter
import dev.jdtech.jellyfin.models.JellyCastEpisode
import dev.jdtech.jellyfin.models.JellyCastItem
import dev.jdtech.jellyfin.models.JellyCastMovie
import dev.jdtech.jellyfin.models.JellyCastSourceType
import dev.jdtech.jellyfin.models.JellyCastSources
import dev.jdtech.jellyfin.player.core.domain.models.ExternalSubtitle
import dev.jdtech.jellyfin.player.core.domain.models.PlayerChapter
import dev.jdtech.jellyfin.player.core.domain.models.PlayerItem
import dev.jdtech.jellyfin.player.core.domain.models.TrickplayInfo
import dev.jdtech.jellyfin.repository.JellyfinRepository
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemFields
import org.jellyfin.sdk.model.api.MediaStreamType
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

class PlaylistManager
@Inject
internal constructor(
    private val repository: JellyfinRepository,
) {
    private var startItem: JellyCastItem? = null
    private var items: List<JellyCastItem> = emptyList()
    private val playerItems: MutableList<PlayerItem> = mutableListOf()
    var currentItemIndex: Int = 0

    suspend fun getInitialItem(
        itemId: UUID,
        itemKind: BaseItemKind,
        mediaSourceIndex: Int? = null,
        startFromBeginning: Boolean = false,
    ): PlayerItem? {
        Timber.Forest.d("Retrieving initial player item")

        val initialItem = when (itemKind) {
            BaseItemKind.MOVIE -> {
                val movie = repository.getMovie(itemId)

                items = listOf(movie)
                movie
            }
            BaseItemKind.SERIES -> {
                val season = repository.getSeasons(itemId).first()
                val episodes = repository.getEpisodes(
                    seriesId = itemId,
                    seasonId = season.id,
                    fields = listOf(
                        ItemFields.CHAPTERS,
                        ItemFields.TRICKPLAY,
                    ),
                ).filter { !it.missing }

                val episode = repository.getNextUp(itemId).firstOrNull() ?: episodes.first()

                items = episodes
                episode
            }
            BaseItemKind.SEASON -> {
                val season = repository.getSeason(itemId)
                val episodes = repository.getEpisodes(
                    seriesId = season.seriesId,
                    seasonId = season.id,
                    fields = listOf(
                        ItemFields.CHAPTERS,
                        ItemFields.TRICKPLAY,
                    ),
                ).filter { !it.missing }

                val episode = repository.getNextUp(season.seriesId).firstOrNull() ?: episodes.first()

                items = episodes
                episode
            }
            BaseItemKind.EPISODE -> {
                val episode = repository.getEpisode(itemId)

                val episodes = repository.getEpisodes(
                    seriesId = episode.seriesId,
                    seasonId = episode.seasonId,
                    fields = listOf(
                        ItemFields.CHAPTERS,
                        ItemFields.TRICKPLAY,
                    ),
                ).filter { !it.missing }

                items = episodes
                episode
            }
            else -> null
        }

        if (initialItem == null) {
            return null
        }

        startItem = initialItem

        currentItemIndex = items.indexOfFirst { it.id == initialItem.id }

        val playbackPosition = if (!startFromBeginning) initialItem.playbackPositionTicks.div(10000) else 0
        val playerItem = initialItem.toPlayerItem(mediaSourceIndex, playbackPosition)
        playerItems.add(playerItem)

        return playerItem
    }

    suspend fun getPreviousPlayerItem(): PlayerItem? {
        Timber.Forest.d("Retrieving previous player item")

        val itemIndex = currentItemIndex - 1
        val playerItem = when (startItem) {
            is JellyCastMovie -> null
            is JellyCastEpisode -> {
                if (currentItemIndex == 0) {
                    null
                } else {
                    val item = items[itemIndex]
                    if (playerItems.firstOrNull { it.itemId == item.id } == null) {
                        try {
                            item.toPlayerItem(0, 0L)
                        } catch (e: Exception) {
                            Timber.e("Failed to retrieve previous player item: $e")
                            null
                        }
                    } else {
                        null
                    }
                }
            }
            else -> null
        }

        if (playerItem != null) {
            playerItems.add(playerItem)
        }

        return playerItem
    }

    suspend fun getNextPlayerItem(): PlayerItem? {
        Timber.Forest.d("Retrieving next player item")

        val itemIndex = currentItemIndex + 1
        val playerItem = when (startItem) {
            is JellyCastMovie -> null
            is JellyCastEpisode -> {
                if (currentItemIndex == items.lastIndex) {
                    null
                } else {
                    val item = items[itemIndex]
                    if (playerItems.firstOrNull { it.itemId == item.id } == null) {
                        try {
                            item.toPlayerItem(0, 0L)
                        } catch (e: Exception) {
                            Timber.e("Failed to retrieve next player item: $e")
                            null
                        }
                    } else {
                        null
                    }
                }
            }
            else -> null
        }

        if (playerItem != null) {
            playerItems.add(playerItem)
        }

        return playerItem
    }

    fun setCurrentMediaItemIndex(itemId: UUID) {
        currentItemIndex = items.indexOfFirst { it.id == itemId }
    }

    private suspend fun JellyCastItem.toPlayerItem(
        mediaSourceIndex: Int?,
        playbackPosition: Long,
    ): PlayerItem {
        Timber.d("Converting JellyCastItem ${this.id} to PlayerItem")

        val mediaSources = repository.getMediaSources(id, true)
        val mediaSource = if (mediaSourceIndex == null) {
            mediaSources.firstOrNull { it.type == JellyCastSourceType.LOCAL } ?: mediaSources[0]
        } else {
            mediaSources[mediaSourceIndex]
        }
        val externalSubtitles = mediaSource.mediaStreams
            .filter { mediaStream ->
                mediaStream.isExternal && mediaStream.type == MediaStreamType.SUBTITLE && !mediaStream.path.isNullOrBlank()
            }
            .map { mediaStream ->
                ExternalSubtitle(
                    mediaStream.title,
                    mediaStream.language,
                    mediaStream.path!!.toUri(),
                    when (mediaStream.codec) {
                        "subrip" -> MimeTypes.APPLICATION_SUBRIP
                        "webvtt" -> MimeTypes.APPLICATION_SUBRIP
                        "ass" -> MimeTypes.TEXT_SSA
                        else -> MimeTypes.TEXT_UNKNOWN
                    },
                )
            }
        val trickplayInfo = when (this) {
            is JellyCastSources -> {
                this.trickplayInfo?.get(mediaSource.id)?.let {
                    TrickplayInfo(
                        width = it.width,
                        height = it.height,
                        tileWidth = it.tileWidth,
                        tileHeight = it.tileHeight,
                        thumbnailCount = it.thumbnailCount,
                        interval = it.interval,
                        bandwidth = it.bandwidth,
                    )
                }
            }
            else -> null
        }
        return PlayerItem(
            name = name,
            itemId = id,
            mediaSourceId = mediaSource.id,
            mediaSourceUri = mediaSource.path,
            playbackPosition = playbackPosition,
            parentIndexNumber = if (this is JellyCastEpisode) parentIndexNumber else null,
            indexNumber = if (this is JellyCastEpisode) indexNumber else null,
            indexNumberEnd = if (this is JellyCastEpisode) indexNumberEnd else null,
            externalSubtitles = externalSubtitles,
            chapters = chapters.toPlayerChapters(),
            trickplayInfo = trickplayInfo,
        )
    }

    private fun List<JellyCastChapter>.toPlayerChapters(): List<PlayerChapter> {
        return this.map { chapter ->
            PlayerChapter(
                startPosition = chapter.startPosition,
                name = chapter.name,
            )
        }
    }
}
