package dev.jdtech.jellyfin.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.Constants
import dev.jdtech.jellyfin.core.R
import dev.jdtech.jellyfin.models.FavoriteSection
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidMovie
import dev.jdtech.jellyfin.models.FindroidShow
import dev.jdtech.jellyfin.models.UiText
import dev.jdtech.jellyfin.repository.JellyfinRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SearchResultViewModel
@Inject
constructor(
    private val jellyfinRepository: JellyfinRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    sealed class UiState {
        data class Normal(val sections: List<FavoriteSection>) : UiState()
        data object Loading : UiState()
        data class Error(val error: Exception) : UiState()
    }

    fun loadData(query: String) {
        viewModelScope.launch {
            _uiState.emit(UiState.Loading)
            try {
                val items = jellyfinRepository.getSearchItems(query)

                if (items.isEmpty()) {
                    _uiState.emit(UiState.Normal(emptyList()))
                    return@launch
                }

                val sections = mutableListOf<FavoriteSection>()

                withContext(Dispatchers.Default) {
                    FavoriteSection(
                        Constants.FAVORITE_TYPE_MOVIES,
                        UiText.StringResource(R.string.movies_label),
                        items.filterIsInstance<FindroidMovie>(),
                    ).let {
                        if (it.items.isNotEmpty()) {
                            sections.add(
                                it,
                            )
                        }
                    }
                    FavoriteSection(
                        Constants.FAVORITE_TYPE_SHOWS,
                        UiText.StringResource(R.string.shows_label),
                        items.filterIsInstance<FindroidShow>(),
                    ).let {
                        if (it.items.isNotEmpty()) {
                            sections.add(
                                it,
                            )
                        }
                    }
                    FavoriteSection(
                        Constants.FAVORITE_TYPE_EPISODES,
                        UiText.StringResource(R.string.episodes_label),
                        items.filterIsInstance<FindroidEpisode>(),
                    ).let {
                        if (it.items.isNotEmpty()) {
                            sections.add(
                                it,
                            )
                        }
                    }
                }

                _uiState.emit(UiState.Normal(sections))
            } catch (e: Exception) {
                _uiState.emit(UiState.Error(e))
            }
        }
    }
}
