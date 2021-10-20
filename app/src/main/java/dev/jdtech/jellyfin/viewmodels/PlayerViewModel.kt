package dev.jdtech.jellyfin.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.models.PlayerItem
import dev.jdtech.jellyfin.repository.JellyfinRepository
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

    private val playerItems = MutableLiveData<PlayerItemState>()

    fun playerItems(): LiveData<PlayerItemState> = playerItems

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
                PlayerItemError(e.message.orEmpty())
            }

            playerItems.postValue(items)
        }
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
        "Series" -> itemToPlayerItems(item, playbackPosition, mediaSourceIndex)
        "Episode" -> itemToPlayerItems(item, playbackPosition, mediaSourceIndex)
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

    private suspend fun itemToPlayerItems(
        item: BaseItemDto,
        playbackPosition: Long,
        mediaSourceIndex: Int
    ): List<PlayerItem> {
        val nextUp = repository.getNextUp(item.seriesId)

        return if (nextUp.isEmpty()) {
            repository
                .getSeasons(item.seriesId!!)
                .flatMap { episodesToPlayerItems(item, playbackPosition, mediaSourceIndex) }
        } else {
            episodesToPlayerItems(item, playbackPosition, mediaSourceIndex)
        }
    }

    private suspend fun episodesToPlayerItems(
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

    data class PlayerItemError(val message: String): PlayerItemState()
    data class PlayerItems(val items: List<PlayerItem>): PlayerItemState()
}