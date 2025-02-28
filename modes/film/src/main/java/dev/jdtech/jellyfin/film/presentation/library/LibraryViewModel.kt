package dev.jdtech.jellyfin.film.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.models.CollectionType
import dev.jdtech.jellyfin.models.SortBy
import dev.jdtech.jellyfin.repository.JellyfinRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.SortOrder
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel
@Inject
constructor(
    private val jellyfinRepository: JellyfinRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(LibraryState())
    val state = _state.asStateFlow()

    fun loadItems(
        parentId: UUID,
        libraryType: CollectionType,
        sortBy: SortBy = SortBy.defaultValue,
        sortOrder: SortOrder = SortOrder.ASCENDING,
    ) {
        val itemType = when (libraryType) {
            CollectionType.Movies -> listOf(BaseItemKind.MOVIE)
            CollectionType.TvShows -> listOf(BaseItemKind.SERIES)
            CollectionType.BoxSets -> listOf(BaseItemKind.BOX_SET)
            CollectionType.Mixed -> listOf(BaseItemKind.FOLDER, BaseItemKind.MOVIE, BaseItemKind.SERIES)
            else -> null
        }

        val recursive = itemType == null || !itemType.contains(BaseItemKind.FOLDER)

        viewModelScope.launch {
            _state.emit(_state.value.copy(isLoading = true, error = null))
            try {
                val items = jellyfinRepository.getItemsPaging(
                    parentId = parentId,
                    includeTypes = itemType,
                    recursive = recursive,
                    sortBy = if (libraryType == CollectionType.TvShows && sortBy == SortBy.DATE_PLAYED) SortBy.SERIES_DATE_PLAYED else sortBy, // Jellyfin uses a different enum for sorting series by data played
                    sortOrder = sortOrder,
                ).cachedIn(viewModelScope)
                _state.emit(_state.value.copy(items = items))
            } catch (e: Exception) {
                _state.emit(_state.value.copy(error = e))
            }
        }
    }
}
