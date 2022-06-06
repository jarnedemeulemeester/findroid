package dev.jdtech.jellyfin.viewmodels

import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.models.FavoriteSection
import dev.jdtech.jellyfin.repository.JellyfinRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject

@HiltViewModel
class SearchResultViewModel
@Inject
constructor(
    private val jellyfinRepository: JellyfinRepository
) : ViewModel() {
    private val uiState = MutableStateFlow<UiState>(UiState.Loading)

    sealed class UiState {
        data class Normal(val sections: List<FavoriteSection>) : UiState()
        object Loading : UiState()
        data class Error(val error: Exception) : UiState()
    }

    fun onUiState(scope: LifecycleCoroutineScope, collector: (UiState) -> Unit) {
        scope.launch { uiState.collect { collector(it) } }
    }

    fun loadData(query: String) {
        viewModelScope.launch {
            uiState.emit(UiState.Loading)
            try {
                val items = jellyfinRepository.getSearchItems(query)

                if (items.isEmpty()) {
                    uiState.emit(UiState.Normal(emptyList()))
                    return@launch
                }

                val sections = mutableListOf<FavoriteSection>()

                withContext(Dispatchers.Default) {
                    FavoriteSection(
                        UUID.randomUUID(),
                        "Movies",
                        items.filter { it.type == "Movie" }).let {
                        if (it.items.isNotEmpty()) sections.add(
                            it
                        )
                    }
                    FavoriteSection(
                        UUID.randomUUID(),
                        "Shows",
                        items.filter { it.type == "Series" }).let {
                        if (it.items.isNotEmpty()) sections.add(
                            it
                        )
                    }
                    FavoriteSection(
                        UUID.randomUUID(),
                        "Episodes",
                        items.filter { it.type == "Episode" }).let {
                        if (it.items.isNotEmpty()) sections.add(
                            it
                        )
                    }
                }

                uiState.emit(UiState.Normal(sections))
            } catch (e: Exception) {
                uiState.emit(UiState.Error(e))
            }
        }
    }
}