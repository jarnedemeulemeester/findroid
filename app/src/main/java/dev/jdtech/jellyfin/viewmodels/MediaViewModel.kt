package dev.jdtech.jellyfin.viewmodels

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.models.CollectionType
import dev.jdtech.jellyfin.repository.JellyfinRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.BaseItemDto
import javax.inject.Inject

@HiltViewModel
class MediaViewModel
@Inject
constructor(
    private val jellyfinRepository: JellyfinRepository
) : ViewModel() {

    private val uiState = MutableStateFlow<UiState>(UiState.Loading)

    sealed class UiState {
        data class Normal(val collections: List<BaseItemDto>) : UiState()
        object Loading : UiState()
        data class Error(val error: Exception) : UiState()
    }

    fun onUiState(scope: LifecycleCoroutineScope, collector: (UiState) -> Unit) {
        scope.launch { uiState.collect { collector(it) } }
    }

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            uiState.emit(UiState.Loading)
            try {
                val items = jellyfinRepository.getItems()
                val collections =
                    items.filter { collection -> CollectionType.unsupportedCollections.none { it.type == collection.collectionType } }
                uiState.emit(UiState.Normal(collections))
            } catch (e: Exception) {
                uiState.emit(
                    UiState.Error(e)
                )
            }
        }
    }
}