package dev.jdtech.jellyfin.film.presentation.person

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.models.FindroidMovie
import dev.jdtech.jellyfin.models.FindroidShow
import dev.jdtech.jellyfin.models.toFindroidPerson
import dev.jdtech.jellyfin.repository.JellyfinRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.BaseItemKind
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class PersonViewModel @Inject internal constructor(
    private val repository: JellyfinRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(PersonState())
    val state = _state.asStateFlow()

    fun loadPerson(personId: UUID) {
        viewModelScope.launch {
            try {
                val personitem = repository.getItem(personId)
                val person = personitem.toFindroidPerson(repository)

                val items = repository.getPersonItems(
                    personIds = listOf(personId),
                    includeTypes = listOf(BaseItemKind.MOVIE, BaseItemKind.SERIES),
                    recursive = true,
                )

                val movies = items.filterIsInstance<FindroidMovie>()
                val shows = items.filterIsInstance<FindroidShow>()

                _state.emit(_state.value.copy(person = person, starredInMovies = movies, starredInShows = shows))
            } catch (e: Exception) {
                _state.emit(_state.value.copy(error = e))
            }
        }
    }
}
