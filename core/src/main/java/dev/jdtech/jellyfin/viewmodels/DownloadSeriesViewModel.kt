package dev.jdtech.jellyfin.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.models.DownloadEpisodeItem
import dev.jdtech.jellyfin.models.DownloadSeriesMetadata
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

    fun loadEpisodes(seriesMetadata: dev.jdtech.jellyfin.models.DownloadSeriesMetadata) {
        viewModelScope.launch {
            _uiState.emit(UiState.Loading)
            try {
                _uiState.emit(UiState.Normal(getEpisodes((seriesMetadata))))
            } catch (e: Exception) {
                _uiState.emit(UiState.Error(e))
            }
        }
    }

    private fun getEpisodes(seriesMetadata: dev.jdtech.jellyfin.models.DownloadSeriesMetadata): List<DownloadEpisodeItem> {
        val episodes = seriesMetadata.episodes
        return listOf(DownloadEpisodeItem.Header) + episodes.sortedWith(
            compareBy(
                { it.item!!.parentIndexNumber },
                { it.item!!.indexNumber }
            )
        ).map { DownloadEpisodeItem.Episode(it) }
    }
}
