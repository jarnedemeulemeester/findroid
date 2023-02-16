package dev.jdtech.jellyfin.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.Constants
import dev.jdtech.jellyfin.core.R
import dev.jdtech.jellyfin.models.FavoriteSection
import dev.jdtech.jellyfin.models.UiText
import dev.jdtech.jellyfin.repository.JellyfinRepository
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.model.api.BaseItemKind

@HiltViewModel
class CollectionViewModel
@Inject
constructor(
    private val jellyfinRepository: JellyfinRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    sealed class UiState {
        data class Normal(val collectionSections: List<FavoriteSection>) : UiState()
        object Loading : UiState()
        data class Error(val error: Exception) : UiState()
    }

    fun loadItems(parentId: UUID) {
        viewModelScope.launch {
            _uiState.emit(UiState.Loading)

            try {
                val items = jellyfinRepository.getItems(parentId = parentId)

                if (items.isEmpty()) {
                    _uiState.emit(UiState.Normal(emptyList()))
                    return@launch
                }

                val favoriteSections = mutableListOf<FavoriteSection>()

                withContext(Dispatchers.Default) {
                    FavoriteSection(
                        Constants.FAVORITE_TYPE_MOVIES,
                        UiText.StringResource(R.string.movies_label),
                        items.filter { it.type == BaseItemKind.MOVIE }
                    ).let {
                        if (it.items.isNotEmpty()) favoriteSections.add(
                            it
                        )
                    }
                    FavoriteSection(
                        Constants.FAVORITE_TYPE_SHOWS,
                        UiText.StringResource(R.string.shows_label),
                        items.filter { it.type == BaseItemKind.SERIES }
                    ).let {
                        if (it.items.isNotEmpty()) favoriteSections.add(
                            it
                        )
                    }
                    FavoriteSection(
                        Constants.FAVORITE_TYPE_EPISODES,
                        UiText.StringResource(R.string.episodes_label),
                        items.filter { it.type == BaseItemKind.EPISODE }
                    ).let {
                        if (it.items.isNotEmpty()) favoriteSections.add(
                            it
                        )
                    }
                }

                _uiState.emit(UiState.Normal(favoriteSections))
            } catch (e: Exception) {
                _uiState.emit(UiState.Error(e))
            }
        }
    }
}
