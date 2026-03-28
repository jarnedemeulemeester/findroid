package dev.jdtech.jellyfin.film.presentation.season

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.models.FindroidSourceType
import dev.jdtech.jellyfin.models.isDownloaded
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import dev.jdtech.jellyfin.utils.Downloader
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.ItemFields

@HiltViewModel
class SeasonViewModel
@Inject
constructor(
    private val repository: JellyfinRepository,
    private val downloader: Downloader,
    private val appPreferences: AppPreferences,
) : ViewModel() {
    private val _state = MutableStateFlow(SeasonState())
    val state = _state.asStateFlow()

    lateinit var seasonId: UUID

    fun loadSeason(seasonId: UUID) {
        this.seasonId = seasonId
        viewModelScope.launch {
            try {
                val season = repository.getSeason(seasonId)
                val episodes =
                    repository.getEpisodes(
                        seriesId = season.seriesId,
                        seasonId = seasonId,
                        fields = listOf(ItemFields.OVERVIEW),
                    )
                _state.emit(_state.value.copy(season = season, episodes = episodes))
            } catch (e: Exception) {
                _state.emit(_state.value.copy(error = e))
            }
        }
    }

    fun downloadSeason() {
        viewModelScope.launch(Dispatchers.IO) {
            val storageIndex =
                appPreferences.getValue(appPreferences.downloadStorageIndex)?.toIntOrNull() ?: 0
            for (episode in _state.value.episodes) {
                if (episode.canDownload && !episode.isDownloaded()) {
                    downloader.downloadItem(
                        item = episode,
                        sourceId = episode.sources.first().id,
                        storageIndex = storageIndex,
                    )
                }
            }
        }
    }

    fun deleteSeasonDownloads() {
        viewModelScope.launch(Dispatchers.IO) {
            for (episode in _state.value.episodes) {
                val localSource =
                    episode.sources.firstOrNull { it.type == FindroidSourceType.LOCAL }
                if (localSource != null) {
                    downloader.deleteItem(item = episode, source = localSource)
                }
            }
            loadSeason(seasonId)
        }
    }

    fun onAction(action: SeasonAction) {
        when (action) {
            is SeasonAction.MarkAsPlayed -> {
                viewModelScope.launch {
                    repository.markAsPlayed(seasonId)
                    loadSeason(seasonId)
                }
            }
            is SeasonAction.UnmarkAsPlayed -> {
                viewModelScope.launch {
                    repository.markAsUnplayed(seasonId)
                    loadSeason(seasonId)
                }
            }
            is SeasonAction.MarkAsFavorite -> {
                viewModelScope.launch {
                    repository.markAsFavorite(seasonId)
                    loadSeason(seasonId)
                }
            }
            is SeasonAction.UnmarkAsFavorite -> {
                viewModelScope.launch {
                    repository.unmarkAsFavorite(seasonId)
                    loadSeason(seasonId)
                }
            }
            else -> Unit
        }
    }
}
