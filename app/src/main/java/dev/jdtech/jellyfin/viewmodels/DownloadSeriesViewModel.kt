package dev.jdtech.jellyfin.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.adapters.DownloadEpisodeItem
import dev.jdtech.jellyfin.models.DownloadSeriesMetadata
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloadSeriesViewModel
@Inject
constructor() : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    sealed class UiState {
        data class Normal(val downloadEpisodes: List<DownloadEpisodeItem>) : UiState()
        object Loading : UiState()
        data class Error(val error: Exception) : UiState()
    }

    fun loadEpisodes(seriesMetadata: DownloadSeriesMetadata) {
        viewModelScope.launch {
            _uiState.emit(UiState.Loading)
            try {
                _uiState.emit(UiState.Normal(getEpisodes((seriesMetadata))))
            } catch (e: Exception) {
                _uiState.emit(UiState.Error(e))
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