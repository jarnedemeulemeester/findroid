package dev.jdtech.jellyfin.film.presentation.show

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.models.JellyCastEpisode
import dev.jdtech.jellyfin.models.JellyCastItemPerson
import dev.jdtech.jellyfin.models.JellyCastShow
import dev.jdtech.jellyfin.repository.JellyfinRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.model.api.PersonKind
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ShowViewModel
@Inject
constructor(
    private val repository: JellyfinRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ShowState())
    val state = _state.asStateFlow()

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
                _state.emit(_state.value.copy(show = show, nextUp = nextUp, seasons = seasons, actors = actors, director = director, writers = writers))
            } catch (e: Exception) {
                _state.emit(_state.value.copy(error = e))
            }
        }
    }

    private suspend fun getNextUp(showId: UUID): JellyCastEpisode? {
        val nextUpItems = repository.getNextUp(showId)
        return nextUpItems.getOrNull(0)
    }

    private suspend fun getActors(item: JellyCastShow): List<JellyCastItemPerson> {
        return withContext(Dispatchers.Default) {
            item.people.filter { it.type == PersonKind.ACTOR }
        }
    }

    private suspend fun getDirector(item: JellyCastShow): JellyCastItemPerson? {
        return withContext(Dispatchers.Default) {
            item.people.firstOrNull { it.type == PersonKind.DIRECTOR }
        }
    }

    private suspend fun getWriters(item: JellyCastShow): List<JellyCastItemPerson> {
        return withContext(Dispatchers.Default) {
            item.people.filter { it.type == PersonKind.WRITER }
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
            else -> Unit
        }
    }
}
