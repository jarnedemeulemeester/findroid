package dev.jdtech.jellyfin.viewmodels

import androidx.core.net.toUri
import androidx.media3.common.MimeTypes
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.models.ExternalSubtitle
import dev.jdtech.jellyfin.models.FindroidChapter
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.FindroidMovie
import dev.jdtech.jellyfin.models.FindroidSourceType
import dev.jdtech.jellyfin.models.FindroidSources
import dev.jdtech.jellyfin.models.PlayerChapter
import dev.jdtech.jellyfin.models.PlayerItem
import dev.jdtech.jellyfin.models.TrickplayInfo
import dev.jdtech.jellyfin.models.toFindroidEpisode
import dev.jdtech.jellyfin.models.toFindroidMovie
import dev.jdtech.jellyfin.models.toFindroidSeason
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
    private val database: ServerDatabaseDao,
) {
    lateinit var startItem: FindroidItem
    lateinit var items: List<FindroidItem>
    lateinit var playerItems: MutableList<PlayerItem?>
    var currentItemIndex: Int = 0

    suspend fun getInitialItem(
        itemId: UUID,
        mediaSourceIndex: Int? = null,
        startFromBeginning: Boolean = false,
    ): PlayerItem? {
        Timber.d("Retrieving initial player item")

        val item = repository.getItem(itemId)

        val playerItem = when (item.type) {
            BaseItemKind.MOVIE -> {
                val movie = item.toFindroidMovie(repository, database)
                val playbackPosition = if (!startFromBeginning) movie.playbackPositionTicks.div(10000) else 0

                startItem = movie
                items = listOf(movie)
                movie.toPlayerItem(mediaSourceIndex, playbackPosition)
            }
            BaseItemKind.SERIES -> {
                val season = repository.getSeasons(item.id).first()
                val episodes = repository.getEpisodes(
                    seriesId = item.id,
                    seasonId = season.id,
                    fields = listOf(
                        ItemFields.CHAPTERS,
                        ItemFields.TRICKPLAY,
                    ),
                )

                var episode = repository.getNextUp(item.id).firstOrNull()

                if (episode == null) {
                    episode = episodes.first()
                }

                val playbackPosition = if (!startFromBeginning) episode.playbackPositionTicks.div(10000) else 0

                startItem = episode
                items = episodes
                currentItemIndex = items.indexOfFirst { it.id == episode.id }
                episode.toPlayerItem(mediaSourceIndex, playbackPosition)
            }
            BaseItemKind.SEASON -> {
                val season = item.toFindroidSeason(repository)
                val episodes = repository.getEpisodes(
                    seriesId = season.seriesId,
                    seasonId = season.id,
                    fields = listOf(
                        ItemFields.CHAPTERS,
                        ItemFields.TRICKPLAY,
                    ),
                )

                val episode = episodes.first()

                val playbackPosition = if (!startFromBeginning) episode.playbackPositionTicks.div(10000) else 0

                startItem = episode
                items = episodes
                currentItemIndex = items.indexOfFirst { it.id == episode.id }
                episode.toPlayerItem(mediaSourceIndex, playbackPosition)
            }
            BaseItemKind.EPISODE -> {
                val episode = item.toFindroidEpisode(repository, database) ?: return null

                val episodes = repository.getEpisodes(
                    seriesId = episode.seriesId,
                    seasonId = episode.seasonId,
                    fields = listOf(
                        ItemFields.CHAPTERS,
                        ItemFields.TRICKPLAY,
                    ),
                )

                val playbackPosition = if (!startFromBeginning) episode.playbackPositionTicks.div(10000) else 0

                startItem = episode
                items = episodes
                currentItemIndex = items.indexOfFirst { it.id == episode.id }
                episode.toPlayerItem(mediaSourceIndex, playbackPosition)
            }
            else -> null
        }

        playerItems = items.map { null }.toMutableList()

        if (playerItem != null) {
            playerItems[currentItemIndex] = playerItem
        }

        return playerItem
    }

    suspend fun getPreviousPlayerItem(): PlayerItem? {
        Timber.d("Retrieving previous player item")

        val itemIndex = currentItemIndex - 1
        val playerItem = when (startItem) {
            is FindroidMovie -> null
            is FindroidEpisode -> {
                if (currentItemIndex == 0) {
                    null
                } else {
                    val item = items[itemIndex]
                    if (playerItems.firstOrNull { it?.itemId == item.id } == null) {
                        item.toPlayerItem(0, 0L)
                    } else {
                        null
                    }
                }
            }
            else -> null
        }

        if (playerItem != null) {
            playerItems.add(itemIndex, playerItem)
        }
        return playerItem
    }

    suspend fun getNextPlayerItem(): PlayerItem? {
        Timber.d("Retrieving next player item")

        val itemIndex = currentItemIndex + 1
        val playerItem = when (startItem) {
            is FindroidMovie -> null
            is FindroidEpisode -> {
                if (currentItemIndex == items.lastIndex) {
                    null
                } else {
                    val item = items[itemIndex]
                    if (playerItems.firstOrNull { it?.itemId == item.id } == null) {
                        item.toPlayerItem(0, 0L)
                    } else {
                        null
                    }
                }
            }
            else -> null
        }

        if (playerItem != null) {
            playerItems.add(itemIndex, playerItem)
        }

        return playerItem
    }

    fun setCurrentMediaItemIndex(itemId: UUID) {
        currentItemIndex = items.indexOfFirst { it.id == itemId }
    }

    private suspend fun FindroidItem.toPlayerItem(
        mediaSourceIndex: Int?,
        playbackPosition: Long,
    ): PlayerItem {
        val mediaSources = repository.getMediaSources(id, true)
        val mediaSource = if (mediaSourceIndex == null) {
            mediaSources.firstOrNull { it.type == FindroidSourceType.LOCAL } ?: mediaSources[0]
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
            is FindroidSources -> {
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
            parentIndexNumber = if (this is FindroidEpisode) parentIndexNumber else null,
            indexNumber = if (this is FindroidEpisode) indexNumber else null,
            indexNumberEnd = if (this is FindroidEpisode) indexNumberEnd else null,
            externalSubtitles = externalSubtitles,
            chapters = chapters.toPlayerChapters(),
            trickplayInfo = trickplayInfo,
        )
    }

    private fun List<FindroidChapter>.toPlayerChapters(): List<PlayerChapter> {
        return this.map { chapter ->
            PlayerChapter(
                startPosition = chapter.startPosition,
                name = chapter.name,
            )
        }
    }
}
