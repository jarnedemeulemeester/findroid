package dev.jdtech.jellyfin.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.models.CollectionType
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.SortBy
import dev.jdtech.jellyfin.repository.JellyfinRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.SortOrder
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel
@Inject
constructor(
    private val jellyfinRepository: JellyfinRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    var itemsloaded = false

    sealed class UiState {
        data class Normal(val items: Flow<PagingData<FindroidItem>>) : UiState()
        data object Loading : UiState()
        data class Error(val error: Exception) : UiState()
    }

    fun loadItems(
        parentId: UUID,
        libraryType: CollectionType,
        sortBy: SortBy = SortBy.defaultValue,
        sortOrder: SortOrder = SortOrder.ASCENDING,
    ) {
        itemsloaded = true
        Timber.d("$libraryType")
        val itemType = when (libraryType) {
            CollectionType.Movies -> listOf(BaseItemKind.MOVIE)
            CollectionType.TvShows -> listOf(BaseItemKind.SERIES)
            CollectionType.BoxSets -> listOf(BaseItemKind.BOX_SET)
            else -> null
        }
        viewModelScope.launch {
            _uiState.emit(UiState.Loading)
            try {
                val items = jellyfinRepository.getItemsPaging(
                    parentId = parentId,
                    includeTypes = itemType,
                    recursive = true,
                    sortBy = sortBy,
                    sortOrder = sortOrder,
                ).cachedIn(viewModelScope)
                _uiState.emit(UiState.Normal(items))
            } catch (e: Exception) {
                _uiState.emit(UiState.Error(e))
            }
        }
    }
}
