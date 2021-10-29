package dev.jdtech.jellyfin.viewmodels

import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.models.PlayerItem
import dev.jdtech.jellyfin.repository.JellyfinRepository
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.ItemFields
import org.jellyfin.sdk.model.api.LocationType.VIRTUAL
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject internal constructor(
    private val repository: JellyfinRepository
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
        onVersionSelectRequired: () -> Unit = { Unit }
    ) {
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
                PlayerItemError(e.toString())
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
            .map { intro ->
                PlayerItem(
                    intro.name,
                    intro.id,
                    intro.mediaSources?.get(0)?.id!!,
                    0
                )
            }
    }

    private suspend fun prepareMediaPlayerItems(
        item: BaseItemDto,
        playbackPosition: Long,
        mediaSourceIndex: Int
    ): List<PlayerItem> = when (item.type) {
        "Movie" -> itemToMoviePlayerItems(item, playbackPosition, mediaSourceIndex)
        "Series" -> seriesToPlayerItems(item, playbackPosition, mediaSourceIndex)
        "Episode" -> episodeToPlayerItems(item, playbackPosition, mediaSourceIndex)
        else -> emptyList()
    }

    private fun itemToMoviePlayerItems(
        item: BaseItemDto,
        playbackPosition: Long,
        mediaSourceIndex: Int
    ) = listOf(
        PlayerItem(
            item.name,
            item.id,
            item.mediaSources?.get(mediaSourceIndex)?.id!!,
            playbackPosition
        )
    )

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
        val episodes = repository.getEpisodes(
            seriesId = item.seriesId!!,
            seasonId = item.id,
            fields = listOf(ItemFields.MEDIA_SOURCES)
        )

        return episodes
            .filter { it.mediaSources != null && it.mediaSources?.isNotEmpty() == true }
            .filter { it.locationType != VIRTUAL }
            .map { episode ->
                PlayerItem(
                    episode.name,
                    episode.id,
                    episode.mediaSources?.get(mediaSourceIndex)?.id!!,
                    playbackPosition
                )
            }
    }

    private suspend fun episodeToPlayerItems(
        item: BaseItemDto,
        playbackPosition: Long,
        mediaSourceIndex: Int
    ): List<PlayerItem> {
        val episodes = repository.getEpisodes(
            seriesId = item.seriesId!!,
            seasonId = item.seasonId!!,
            fields = listOf(ItemFields.MEDIA_SOURCES),
            startItemId = item.id
        )

        return episodes
            .filter { it.mediaSources != null && it.mediaSources?.isNotEmpty() == true }
            .filter { it.locationType != VIRTUAL }
            .map { episode ->
                PlayerItem(
                    episode.name,
                    episode.id,
                    episode.mediaSources?.get(mediaSourceIndex)?.id!!,
                    playbackPosition
                )
            }
    }

    sealed class PlayerItemState

    data class PlayerItemError(val message: String) : PlayerItemState()
    data class PlayerItems(val items: List<PlayerItem>) : PlayerItemState()
}