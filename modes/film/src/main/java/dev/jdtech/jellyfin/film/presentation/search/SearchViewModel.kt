package dev.jdtech.jellyfin.film.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.repository.JellyfinRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SearchViewModel
@Inject
constructor(
    private val repository: JellyfinRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(SearchState())
    val state = _state.asStateFlow()

    private fun search(query: String) {
        viewModelScope.launch {
            if (query.isBlank()) {
                _state.emit(SearchState(items = emptyList(), loading = false))
                return@launch
            }

            _state.emit(_state.value.copy(loading = true))
            try {
                val items = repository.getSearchItems(query)

                _state.emit(SearchState(items = items, loading = false))
            } catch (e: Exception) {
                Timber.e(e)
                _state.emit(_state.value.copy(loading = false))
            }
        }
    }

    fun onAction(action: SearchAction) {
        when (action) {
            is SearchAction.Search -> {
                search(query = action.query)
            }
            else -> Unit
        }
    }
}
