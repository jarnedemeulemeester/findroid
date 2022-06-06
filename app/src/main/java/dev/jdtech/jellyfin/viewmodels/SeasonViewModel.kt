package dev.jdtech.jellyfin.viewmodels

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.adapters.EpisodeItem
import dev.jdtech.jellyfin.repository.JellyfinRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.ItemFields
import java.util.*
import javax.inject.Inject

@HiltViewModel
class SeasonViewModel
@Inject
constructor(
    private val jellyfinRepository: JellyfinRepository
) : ViewModel() {
    private val uiState = MutableStateFlow<UiState>(UiState.Loading)

    sealed class UiState {
        data class Normal(val episodes: List<EpisodeItem>) : UiState()
        object Loading : UiState()
        data class Error(val error: Exception) : UiState()
    }

    fun onUiState(scope: LifecycleCoroutineScope, collector: (UiState) -> Unit) {
        scope.launch { uiState.collect { collector(it) } }
    }

    fun loadEpisodes(seriesId: UUID, seasonId: UUID) {
        viewModelScope.launch {
            uiState.emit(UiState.Loading)
            try {
                val episodes = getEpisodes(seriesId, seasonId)
                uiState.emit(UiState.Normal(episodes))
            } catch (e: Exception) {
                uiState.emit(UiState.Error(e))
            }
        }
    }

    private suspend fun getEpisodes(seriesId: UUID, seasonId: UUID): List<EpisodeItem> {
        val episodes =
            jellyfinRepository.getEpisodes(seriesId, seasonId, fields = listOf(ItemFields.OVERVIEW))
        return listOf(EpisodeItem.Header) + episodes.map { EpisodeItem.Episode(it) }
    }
}