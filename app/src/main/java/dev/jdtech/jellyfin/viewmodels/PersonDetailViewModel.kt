package dev.jdtech.jellyfin.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.models.ContentType.MOVIE
import dev.jdtech.jellyfin.models.ContentType.TVSHOW
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.utils.contentType
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.BaseItemDto
import timber.log.Timber
import java.lang.Exception
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
internal class PersonDetailViewModel @Inject internal constructor(
    private val jellyfinRepository: JellyfinRepository
) : ViewModel() {

    val data = MutableLiveData<PersonOverview>()
    val starredIn = MutableLiveData<StarredIn>()

    private val _finishedLoading = MutableLiveData<Boolean>()
    val finishedLoading: LiveData<Boolean> = _finishedLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    fun loadData(personId: UUID) {
        _error.value = null
        _finishedLoading.value = false
        viewModelScope.launch {
            try {
                val personDetail = jellyfinRepository.getItem(personId)

                data.postValue(
                    PersonOverview(
                        name = personDetail.name.orEmpty(),
                        overview = personDetail.overview.orEmpty(),
                        dto = personDetail
                    )
                )
                val items = jellyfinRepository.getPersonItems(
                    personIds = listOf(personId),
                    includeTypes = listOf(MOVIE, TVSHOW),
                    recursive = true
                )

                val movies = items.filter { it.contentType() == MOVIE }
                val shows = items.filter { it.contentType() == TVSHOW }

                starredIn.postValue(StarredIn(movies, shows))

            } catch (e: Exception) {
                Timber.e(e)
                _error.value = e.toString()
            }
            _finishedLoading.value = true
        }
    }

    data class PersonOverview(
        val name: String,
        val overview: String,
        val dto: BaseItemDto
    )

    data class StarredIn(
        val movies: List<BaseItemDto>,
        val shows: List<BaseItemDto>
    )
}