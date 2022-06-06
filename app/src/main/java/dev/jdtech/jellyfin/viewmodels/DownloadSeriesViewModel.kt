package dev.jdtech.jellyfin.viewmodels

import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.adapters.DownloadEpisodeItem
import dev.jdtech.jellyfin.models.DownloadSeriesMetadata
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloadSeriesViewModel
@Inject
constructor() : ViewModel() {
    private val uiState = MutableStateFlow<UiState>(UiState.Loading)

    sealed class UiState {
        data class Normal(val downloadEpisodes: List<DownloadEpisodeItem>) : UiState()
        object Loading : UiState()
        data class Error(val error: Exception) : UiState()
    }

    fun onUiState(scope: LifecycleCoroutineScope, collector: (UiState) -> Unit) {
        scope.launch { uiState.collect { collector(it) } }
    }

    fun loadEpisodes(seriesMetadata: DownloadSeriesMetadata) {
        viewModelScope.launch {
            uiState.emit(UiState.Loading)
            try {
                uiState.emit(UiState.Normal(getEpisodes((seriesMetadata))))
            } catch (e: Exception) {
                uiState.emit(UiState.Error(e))
            }
        }
    }

    private fun getEpisodes(seriesMetadata: DownloadSeriesMetadata): List<DownloadEpisodeItem> {
        val episodes = seriesMetadata.episodes
        return listOf(DownloadEpisodeItem.Header) + episodes.sortedWith(compareBy(
            { it.item!!.parentIndexNumber },
            { it.item!!.indexNumber })).map { DownloadEpisodeItem.Episode(it) }
    }
}