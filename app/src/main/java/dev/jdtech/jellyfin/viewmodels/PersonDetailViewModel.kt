package dev.jdtech.jellyfin.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.models.ContentType.MOVIE
import dev.jdtech.jellyfin.models.ContentType.TVSHOW
import dev.jdtech.jellyfin.models.StarredIn.Movie
import dev.jdtech.jellyfin.models.StarredIn.Show
import dev.jdtech.jellyfin.models.contentType
import dev.jdtech.jellyfin.repository.JellyfinRepository
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.BaseItemDto
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
internal class PersonDetailViewModel @Inject internal constructor(
    private val jellyfinRepository: JellyfinRepository
) : ViewModel() {

    val data = MutableLiveData<PersonOverview>()
    val starredInMovies = MutableLiveData<List<Movie>>()
    val starredInShows = MutableLiveData<List<Show>>()

    fun loadData(personId: UUID) {
        viewModelScope.launch(IO) {
            val personDetail = jellyfinRepository.getItem(personId)

            data.postValue(
                PersonOverview(
                    name = personDetail.name.orEmpty(),
                    overview = personDetail.overview.orEmpty(),
                    dto = personDetail
                )
            )
        }

        viewModelScope.launch(IO) {
            val items = jellyfinRepository.getPersonItems(
                personIds = listOf(personId),
                includeTypes = listOf(MOVIE, TVSHOW),
                recursive = true
            )

            items
                .filter { it.contentType() == MOVIE }
                .map { item -> item.toMovie() }
                .let { starredInMovies.postValue(it) }

            items
                .filter { it.contentType() == TVSHOW }
                .map { item -> item.toShow() }
                .let { starredInShows.postValue(it) }
        }
    }

    private fun BaseItemDto.toMovie() = Movie(
        id = id,
        title = name.orEmpty(),
        released = productionYear?.toString().orEmpty(),
        dto = this
    )

    private fun BaseItemDto.toShow() = Show(
        id = id,
        title = name.orEmpty(),
        released = productionYear?.toString().orEmpty(),
        dto = this
    )

    data class PersonOverview(
        val name: String,
        val overview: String,
        val dto: BaseItemDto
    )
}