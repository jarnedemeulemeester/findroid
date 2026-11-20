package dev.jdtech.jellyfin.film.presentation.show

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidItemPerson
import dev.jdtech.jellyfin.models.FindroidShow
import dev.jdtech.jellyfin.repository.JellyfinRepository
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.model.api.PersonKind
import timber.log.Timber

@HiltViewModel
class ShowViewModel @Inject constructor(private val repository: JellyfinRepository) : ViewModel() {
    private val _state = MutableStateFlow(ShowState())
    val state = _state.asStateFlow()

    private val eventsChannel = Channel<ShowEvent>()
    val events = eventsChannel.receiveAsFlow()

    lateinit var showId: UUID

    fun loadShow(showId: UUID) {
        this.showId = showId
        viewModelScope.launch {
            try {
                val show = repository.getShow(showId)
                val nextUp = getNextUp(showId)
                val seasons = repository.getSeasons(showId)
                val actors = getActors(show)
                val director = getDirector(show)
                val writers = getWriters(show)
                val canDeleteFromServer =
                    try {
                        repository.canDeleteFromServer()
                    } catch (e: Exception) {
                        Timber.e(e)
                        false
                    }
                _state.emit(
                    _state.value.copy(
                        show = show,
                        nextUp = nextUp,
                        seasons = seasons,
                        actors = actors,
                        director = director,
                        writers = writers,
                        canDeleteFromServer = canDeleteFromServer,
                    )
                )
            } catch (e: Exception) {
                _state.emit(_state.value.copy(error = e))
            }
        }
    }

    private suspend fun getNextUp(showId: UUID): FindroidEpisode? {
        val nextUpItems = repository.getNextUp(showId)
        return nextUpItems.getOrNull(0)
    }

    private suspend fun getActors(item: FindroidShow): List<FindroidItemPerson> {
        return withContext(Dispatchers.Default) {
            item.people
                .filter { it.type == PersonKind.ACTOR }
                // Jellyfin 12+ returns a person once per role, keep one card per role
                .distinctBy { it.id to it.role }
        }
    }

    private suspend fun getDirector(item: FindroidShow): FindroidItemPerson? {
        return withContext(Dispatchers.Default) {
            item.people.firstOrNull { it.type == PersonKind.DIRECTOR }
        }
    }

    private suspend fun getWriters(item: FindroidShow): List<FindroidItemPerson> {
        return withContext(Dispatchers.Default) {
            item.people.filter { it.type == PersonKind.WRITER }.distinctBy { it.id }
        }
    }

    fun onAction(action: ShowAction) {
        when (action) {
            is ShowAction.MarkAsPlayed -> {
                viewModelScope.launch {
                    repository.markAsPlayed(showId)
                    loadShow(showId)
                }
            }
            is ShowAction.UnmarkAsPlayed -> {
                viewModelScope.launch {
                    repository.markAsUnplayed(showId)
                    loadShow(showId)
                }
            }
            is ShowAction.MarkAsFavorite -> {
                viewModelScope.launch {
                    repository.markAsFavorite(showId)
                    loadShow(showId)
                }
            }
            is ShowAction.UnmarkAsFavorite -> {
                viewModelScope.launch {
                    repository.unmarkAsFavorite(showId)
                    loadShow(showId)
                }
            }
            is ShowAction.LoadEpisodesForDeletion -> loadEpisodesForDeletion()
            is ShowAction.DeleteEpisodes -> deleteEpisodes(action.episodeIds)
            else -> Unit
        }
    }

    private fun loadEpisodesForDeletion() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingEpisodes = true) }
            try {
                val seasons = _state.value.seasons
                val episodes =
                    seasons
                        .flatMap { season ->
                            repository.getEpisodes(seriesId = showId, seasonId = season.id)
                        }
                        .sortedWith(
                            compareBy<FindroidEpisode>(
                                { it.parentIndexNumber },
                                { it.indexNumber },
                            )
                        )
                _state.update {
                    it.copy(episodesForDeletion = episodes, isLoadingEpisodes = false)
                }
            } catch (e: Exception) {
                Timber.e(e)
                _state.update { it.copy(isLoadingEpisodes = false) }
                eventsChannel.send(ShowEvent.EpisodesLoadFailed)
            }
        }
    }

    private fun deleteEpisodes(episodeIds: List<UUID>) {
        if (episodeIds.isEmpty()) return
        viewModelScope.launch {
            val deletedCount =
                try {
                    repository.deleteItems(episodeIds)
                } catch (e: Exception) {
                    Timber.e(e)
                    eventsChannel.send(ShowEvent.DeleteFailed)
                    return@launch
                }
            if (deletedCount == 0) {
                eventsChannel.send(ShowEvent.DeleteFailed)
            } else {
                eventsChannel.send(
                    ShowEvent.EpisodesDeleted(
                        deletedCount = deletedCount,
                        requestedCount = episodeIds.size,
                    )
                )
            }
        }
    }
}
