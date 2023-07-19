package dev.jdtech.jellyfin.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.models.CollectionType
import dev.jdtech.jellyfin.models.FindroidCollection
import dev.jdtech.jellyfin.repository.JellyfinRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MediaViewModel
@Inject
constructor(
    private val jellyfinRepository: JellyfinRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    sealed class UiState {
        data class Normal(val collections: List<FindroidCollection>) : UiState()
        data object Loading : UiState()
        data class Error(val error: Exception) : UiState()
    }

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.emit(UiState.Loading)
            try {
                val items = jellyfinRepository.getLibraries()
                val collections =
                    items.filter { collection -> collection.type in CollectionType.supported }
                _uiState.emit(UiState.Normal(collections))
            } catch (e: Exception) {
                _uiState.emit(
                    UiState.Error(e),
                )
            }
        }
    }
}
