package dev.jdtech.jellyfin.viewmodels

import android.net.Uri
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.exoplayer2.util.MimeTypes
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.database.DownloadDatabaseDao
import dev.jdtech.jellyfin.models.ExternalSubtitle
import dev.jdtech.jellyfin.models.PlayerItem
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.utils.getDownloadPlayerItem
import dev.jdtech.jellyfin.utils.isItemAvailable
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemFields
import org.jellyfin.sdk.model.api.LocationType.VIRTUAL
import org.jellyfin.sdk.model.api.MediaProtocol
import org.jellyfin.sdk.model.api.MediaStreamType
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject internal constructor(
    private val repository: JellyfinRepository,
    private val downloadDatabase: DownloadDatabaseDao
) : ViewModel() {

    private val playerItems = MutableSharedFlow<PlayerItemState>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    fun onPlaybackRequested(scope: LifecycleCoroutineScope, collector: (PlayerItemState) -> Unit) {
        scope.launch { playerItems.collect { collector(it) } }
    }

    fun loadPlayerItems(
        item: BaseItemDto,
        mediaSourceIndex: Int = 0,
        onVersionSelectRequired: () -> Unit = { }
    ) {
        if (isItemAvailable(item.id)) {
            val playerItem = getDownloadPlayerItem(downloadDatabase, item.id)
            if (playerItem != null) {
                loadOfflinePlayerItems(playerItem)
                return
            }
        }
        Timber.d("Loading player items for item ${item.id}")
        if (item.mediaSources.orEmpty().size > 1) {
            onVersionSelectRequired()
        }

        viewModelScope.launch {
            val playbackPosition = item.userData?.playbackPositionTicks?.div(10000) ?: 0

            val items = try {
                createItems(item, playbackPosition, mediaSourceIndex).let(::PlayerItems)
            } catch (e: Exception) {
                Timber.d(e)
                PlayerItemError(e)
            }

            playerItems.tryEmit(items)
        }
    }

    fun loadOfflinePlayerItems(
        playerItem: PlayerItem
    ) {
        playerItems.tryEmit(PlayerItems(listOf(playerItem)))
    }

    private suspend fun createItems(
        item: BaseItemDto,
        playbackPosition: Long,
        mediaSourceIndex: Int
    ) = if (playbackPosition <= 0) {
        prepareIntros(item) + prepareMediaPlayerItems(
            item,
            playbackPosition,
            mediaSourceIndex
        )
    } else {
        prepareMediaPlayerItems(item, playbackPosition, mediaSourceIndex)
    }

    private suspend fun prepareIntros(item: BaseItemDto): List<PlayerItem> {
        return repository
            .getIntros(item.id)
            .filter { it.mediaSources != null && it.mediaSources?.isNotEmpty() == true }
            .map { intro -> intro.toPlayerItem(mediaSourceIndex = 0, playbackPosition = 0) }
    }

    private suspend fun prepareMediaPlayerItems(
        item: BaseItemDto,
        playbackPosition: Long,
        mediaSourceIndex: Int
    ): List<PlayerItem> = when (item.type) {
        BaseItemKind.MOVIE -> itemToMoviePlayerItems(item, playbackPosition, mediaSourceIndex)
        BaseItemKind.SERIES -> seriesToPlayerItems(item, playbackPosition, mediaSourceIndex)
        BaseItemKind.EPISODE -> episodeToPlayerItems(item, playbackPosition, mediaSourceIndex)
        else -> emptyList()
    }

    private suspend fun itemToMoviePlayerItems(
        item: BaseItemDto,
        playbackPosition: Long,
        mediaSourceIndex: Int
    ) = listOf(item.toPlayerItem(mediaSourceIndex, playbackPosition))

    private suspend fun seriesToPlayerItems(
        item: BaseItemDto,
        playbackPosition: Long,
        mediaSourceIndex: Int
    ): List<PlayerItem> {
        val nextUp = repository.getNextUp(item.id)

        return if (nextUp.isEmpty()) {
            repository
                .getSeasons(item.id)
                .flatMap { seasonToPlayerItems(it, playbackPosition, mediaSourceIndex) }
        } else {
            episodeToPlayerItems(nextUp.first(), playbackPosition, mediaSourceIndex)
        }
    }

    private suspend fun seasonToPlayerItems(
        item: BaseItemDto,
        playbackPosition: Long,
        mediaSourceIndex: Int
    ): List<PlayerItem> {
        return repository
            .getEpisodes(
                seriesId = item.seriesId!!,
                seasonId = item.id,
                fields = listOf(ItemFields.MEDIA_SOURCES)
            )
            .filter { it.mediaSources != null && it.mediaSources?.isNotEmpty() == true }
            .filter { it.locationType != VIRTUAL }
            .map { episode -> episode.toPlayerItem(mediaSourceIndex, playbackPosition) }
    }

    private suspend fun episodeToPlayerItems(
        item: BaseItemDto,
        playbackPosition: Long,
        mediaSourceIndex: Int
    ): List<PlayerItem> {
        return repository
            .getEpisodes(
                seriesId = item.seriesId!!,
                seasonId = item.seasonId!!,
                fields = listOf(ItemFields.MEDIA_SOURCES),
                startItemId = item.id
            )
            .filter { it.mediaSources != null && it.mediaSources?.isNotEmpty() == true }
            .filter { it.locationType != VIRTUAL }
            .map { episode -> episode.toPlayerItem(mediaSourceIndex, playbackPosition) }
    }

    private suspend fun BaseItemDto.toPlayerItem(
        mediaSourceIndex: Int,
        playbackPosition: Long
    ): PlayerItem {
        val mediaSource = repository.getMediaSources(id)[mediaSourceIndex]
        val externalSubtitles = mutableListOf<ExternalSubtitle>()
        for (mediaStream in mediaSource.mediaStreams!!) {
            if (mediaStream.isExternal && mediaStream.type == MediaStreamType.SUBTITLE && !mediaStream.deliveryUrl.isNullOrBlank()) {
                externalSubtitles.add(
                    ExternalSubtitle(
                        mediaStream.title.orEmpty(),
                        mediaStream.language.orEmpty(),
                        Uri.parse(repository.getBaseUrl() + mediaStream.deliveryUrl!!),
                        when (mediaStream.codec) {
                            "subrip" -> MimeTypes.APPLICATION_SUBRIP
                            "webvtt" -> MimeTypes.TEXT_VTT
                            "ass" -> MimeTypes.TEXT_SSA
                            else -> MimeTypes.TEXT_UNKNOWN
                        }
                    )
                )
            }
        }
        return when (mediaSource.protocol) {
            MediaProtocol.FILE -> PlayerItem(
                name = name,
                itemId = id,
                mediaSourceId = mediaSource.id!!,
                playbackPosition = playbackPosition,
                parentIndexNumber = parentIndexNumber,
                indexNumber = indexNumber,
                externalSubtitles = externalSubtitles
            )
            MediaProtocol.HTTP -> PlayerItem(
                name = name,
                itemId = id,
                mediaSourceId = mediaSource.id!!,
                mediaSourceUri = mediaSource.path!!,
                playbackPosition = playbackPosition,
                parentIndexNumber = parentIndexNumber,
                indexNumber = indexNumber,
                externalSubtitles = externalSubtitles
            )
            else -> PlayerItem(
                name = name,
                itemId = id,
                mediaSourceId = mediaSource.id!!,
                playbackPosition = playbackPosition,
                parentIndexNumber = parentIndexNumber,
                indexNumber = indexNumber,
                externalSubtitles = externalSubtitles
            )
        }
    }

    sealed class PlayerItemState

    data class PlayerItemError(val error: Exception) : PlayerItemState()
    data class PlayerItems(val items: List<PlayerItem>) : PlayerItemState()
}