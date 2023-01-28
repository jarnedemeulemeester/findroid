package dev.jdtech.jellyfin.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.models.SortBy
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.SortOrder
import timber.log.Timber

@HiltViewModel
class LibraryViewModel
@Inject
constructor(
    private val jellyfinRepository: JellyfinRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    sealed class UiState {
        data class Normal(val items: Flow<PagingData<BaseItemDto>>) : UiState()
        object Loading : UiState()
        data class Error(val error: Exception) : UiState()
    }

    fun loadItems(
        parentId: UUID,
        libraryType: String?,
        sortBy: SortBy = SortBy.defaultValue,
        sortOrder: SortOrder = SortOrder.ASCENDING
    ) {
        Timber.d("$libraryType")
        val itemType = when (libraryType) {
            "movies" -> listOf(BaseItemKind.MOVIE)
            "tvshows" -> listOf(BaseItemKind.SERIES)
            "boxsets" -> listOf(BaseItemKind.BOX_SET)
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
                    sortOrder = sortOrder
                )
                _uiState.emit(UiState.Normal(items))
            } catch (e: Exception) {
                _uiState.emit(UiState.Error(e))
            }
        }
    }
}
