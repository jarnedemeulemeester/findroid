package dev.jdtech.jellyfin.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.models.EpisodeItem
import dev.jdtech.jellyfin.models.FindroidSeason
import dev.jdtech.jellyfin.models.UiText
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.utils.Downloader
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.ItemFields
import java.util.UUID
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class SeasonViewModel
@Inject
constructor(
    private val jellyfinRepository: JellyfinRepository,
    private val downloader: Downloader,
    ) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _downloadStatus = MutableStateFlow(Pair(0, 0))
    val downloadStatus = _downloadStatus.asStateFlow()

    private val _downloadError = MutableSharedFlow<UiText>()
    val downloadError = _downloadError.asSharedFlow()

    private val _navigateBack = MutableSharedFlow<Boolean>()
    val navigateBack = _navigateBack.asSharedFlow()

    sealed class UiState {
        data class Normal(val episodes: List<EpisodeItem>) : UiState()
        data object Loading : UiState()
        data class Error(val error: Exception) : UiState()
    }

    lateinit var season: FindroidSeason

    fun loadEpisodes(seriesId: UUID, seasonId: UUID, offline: Boolean) {
        viewModelScope.launch {
            _uiState.emit(UiState.Loading)
            try {
                season = getSeason(seasonId)
                val episodes = getEpisodes(seriesId, seasonId, offline)
                _uiState.emit(UiState.Normal(episodes))
            } catch (_: NullPointerException) {
                // Navigate back because item does not exist (probably because it's been deleted)
                _navigateBack.emit(true)
            } catch (e: Exception) {
                _uiState.emit(UiState.Error(e))
            }
        }
    }

    fun download(sourceIndex: Int = 0, storageIndex: Int = 0){
        viewModelScope.launch {
            for (episode in jellyfinRepository.getEpisodes(season.seriesId, season.id)) {
                val item = jellyfinRepository.getEpisode(episode.id)
                val result = downloader.downloadItem(item, item.sources[sourceIndex].id, storageIndex)
                if (result.second != null) {
                    _downloadError.emit(result.second!!)
                    break
                }
            }
            // Send one time signal to fragment that the download has been initiated
            _downloadStatus.emit(Pair(10, Random.nextInt()))
        }
    }

    private suspend fun getSeason(seasonId: UUID): FindroidSeason {
        return jellyfinRepository.getSeason(seasonId)
    }

    private suspend fun getEpisodes(seriesId: UUID, seasonId: UUID, offline: Boolean): List<EpisodeItem> {
        val header = EpisodeItem.Header(seriesId = season.seriesId, seasonId = season.id, seriesName = season.seriesName, seasonName = season.name)
        val episodes =
            jellyfinRepository.getEpisodes(seriesId, seasonId, fields = listOf(ItemFields.OVERVIEW), offline = offline)

        return listOf(header) + episodes.map { EpisodeItem.Episode(it) }
    }
}
