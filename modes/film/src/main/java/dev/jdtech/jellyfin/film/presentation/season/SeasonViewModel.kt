package dev.jdtech.jellyfin.film.presentation.season

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidSourceType
import dev.jdtech.jellyfin.models.isDownloaded
import dev.jdtech.jellyfin.models.isDownloading
import dev.jdtech.jellyfin.repository.JellyfinRepository
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
constructor(private val repository: JellyfinRepository, private val downloader: Downloader) :
    ViewModel() {
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

    fun downloadSeason(storageIndex: Int) {
        val episodeIds = _state.value.episodes.map { it.id }
        if (episodeIds.isEmpty()) {
            return
        }
        viewModelScope.launch(Dispatchers.IO) { downloadEpisodes(episodeIds, storageIndex) }
    }

    fun deleteSeasonDownloads() {
        val season = _state.value.season ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val episodes =
                runCatching {
                        repository.getEpisodes(
                            seriesId = season.seriesId,
                            seasonId = season.id,
                            offline = true,
                        )
                    }
                    .getOrElse { emptyList() }
            deleteEpisodes(episodes)
            loadSeason(season.id)
        }
    }

    private suspend fun downloadEpisodes(episodeIds: List<UUID>, storageIndex: Int) {
        for (episodeId in episodeIds) {
            val episode = runCatching { repository.getEpisode(episodeId) }.getOrNull() ?: continue
            if (!episode.canDownload || episode.isDownloaded() || episode.isDownloading()) {
                continue
            }
            val sourceId =
                episode.sources.firstOrNull()?.id
                    ?: runCatching {
                            repository.getMediaSources(episode.id, true).firstOrNull()?.id
                        }
                        .getOrNull()
            if (sourceId == null) {
                continue
            }
            downloader.downloadItem(
                item = episode,
                sourceId = sourceId,
                storageIndex = storageIndex,
            )
        }
    }

    private suspend fun deleteEpisodes(episodes: List<FindroidEpisode>) {
        for (episode in episodes) {
            if (!episode.isDownloaded() || episode.isDownloading()) {
                continue
            }
            val localSource = episode.sources.firstOrNull { it.type == FindroidSourceType.LOCAL }
            if (localSource == null) {
                continue
            }
            downloader.deleteItem(episode, localSource)
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
