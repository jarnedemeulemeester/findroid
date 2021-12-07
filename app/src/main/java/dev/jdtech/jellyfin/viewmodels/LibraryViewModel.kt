package dev.jdtech.jellyfin.viewmodels

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.utils.SortBy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.SortOrder
import timber.log.Timber
import java.util.*
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel
@Inject
constructor(
    private val jellyfinRepository: JellyfinRepository
) : ViewModel() {
    private val uiState = MutableStateFlow<UiState>(UiState.Loading)

    sealed class UiState {
        data class Normal(val items: List<BaseItemDto>) : UiState()
        object Loading : UiState()
        data class Error(val message: String?) : UiState()
    }

    fun onUiState(scope: LifecycleCoroutineScope, collector: (UiState) -> Unit) {
        scope.launch { uiState.collect { collector(it) } }
    }

    fun loadItems(
        parentId: UUID,
        libraryType: String?,
        sortBy: SortBy = SortBy.defaultValue,
        sortOrder: SortOrder = SortOrder.ASCENDING
    ) {
        Timber.d("$libraryType")
        val itemType = when (libraryType) {
            "movies" -> "Movie"
            "tvshows" -> "Series"
            else -> null
        }
        viewModelScope.launch {
            uiState.emit(UiState.Loading)
            try {
                val items = jellyfinRepository.getItems(
                    parentId,
                    includeTypes = if (itemType != null) listOf(itemType) else null,
                    recursive = true,
                    sortBy = sortBy,
                    sortOrder = sortOrder
                )
                uiState.emit(UiState.Normal(items))
            } catch (e: Exception) {
                uiState.emit(UiState.Error(e.message))
            }
        }
    }
}