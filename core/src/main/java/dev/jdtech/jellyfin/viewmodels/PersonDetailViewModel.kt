package dev.jdtech.jellyfin.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.repository.JellyfinRepository
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind

@HiltViewModel
class PersonDetailViewModel @Inject internal constructor(
    private val jellyfinRepository: JellyfinRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    sealed class UiState {
        data class Normal(val data: PersonOverview, val starredIn: StarredIn) : UiState()
        object Loading : UiState()
        data class Error(val error: Exception) : UiState()
    }

    fun loadData(personId: UUID) {
        viewModelScope.launch {
            _uiState.emit(UiState.Loading)
            try {
                val personDetail = jellyfinRepository.getItem(personId)

                val data = PersonOverview(
                    name = personDetail.name.orEmpty(),
                    overview = personDetail.overview.orEmpty(),
                    dto = personDetail
                )

                val items = jellyfinRepository.getPersonItems(
                    personIds = listOf(personId),
                    includeTypes = listOf(BaseItemKind.MOVIE, BaseItemKind.SERIES),
                    recursive = true
                )

                val movies = items.filter { it.type == BaseItemKind.MOVIE }
                val shows = items.filter { it.type == BaseItemKind.SERIES }

                val starredIn = StarredIn(movies, shows)

                _uiState.emit(UiState.Normal(data, starredIn))
            } catch (e: Exception) {
                _uiState.emit(UiState.Error(e))
            }
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
