package dev.jdtech.jellyfin.film.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.repository.JellyfinRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class SearchViewModel @Inject constructor(private val repository: JellyfinRepository) :
    ViewModel() {
    private val _state = MutableStateFlow(SearchState())
    val state = _state.asStateFlow()

    var currentJob: Job? = null

    private fun search(query: String) {
        currentJob?.cancel()
        currentJob =
            viewModelScope.launch {
                try {
                    if (query.isBlank()) {
                        _state.emit(SearchState(items = emptyList(), loading = false))
                        return@launch
                    }

                    _state.emit(_state.value.copy(loading = true))
                    val items = repository.getSearchItems(query)

                    _state.emit(SearchState(items = items, loading = false))
                } catch (_: CancellationException) {} catch (e: Exception) {
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
