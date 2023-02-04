package dev.jdtech.jellyfin.viewmodels

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.database.DownloadDatabaseDao
import dev.jdtech.jellyfin.models.EpisodeItem
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.utils.requestDownload
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.ItemFields

@HiltViewModel
class SeasonViewModel
@Inject
constructor(
    private val application: Application,
    private val jellyfinRepository: JellyfinRepository,
    private val downloadDatabase: DownloadDatabaseDao
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    sealed class UiState {
        data class Normal(val episodes: List<EpisodeItem>) : UiState()
        object Loading : UiState()
        data class Error(val error: Exception) : UiState()
    }

    fun loadEpisodes(seriesId: UUID, seasonId: UUID) {
        viewModelScope.launch {
            _uiState.emit(UiState.Loading)
            try {
                val episodes = getEpisodes(seriesId, seasonId)
                _uiState.emit(UiState.Normal(episodes))
            } catch (e: Exception) {
                _uiState.emit(UiState.Error(e))
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun download() {
        GlobalScope.launch {
            uiState.collect { uiState ->
                if (uiState !is UiState.Normal) return@collect
                val episodes = uiState.episodes
                episodes.forEach {
                    try{
                        requestDownload(
                            jellyfinRepository,
                            downloadDatabase,
                            application,
                            it.id
                        )
                    } catch (_ : Exception) { }
                }
            }
        }
    }

    private suspend fun getEpisodes(seriesId: UUID, seasonId: UUID): List<EpisodeItem> {
        val episodes =
            jellyfinRepository.getEpisodes(seriesId, seasonId, fields = listOf(ItemFields.OVERVIEW))
        return listOf(EpisodeItem.Header) + episodes.map { EpisodeItem.Episode(it) }
    }
}
