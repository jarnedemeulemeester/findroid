package dev.jdtech.jellyfin.film.presentation.show

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidItemPerson
import dev.jdtech.jellyfin.models.FindroidSeason
import dev.jdtech.jellyfin.models.FindroidShow
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
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.model.api.ItemFields
import org.jellyfin.sdk.model.api.PersonKind

@HiltViewModel
class ShowViewModel
@Inject
constructor(private val repository: JellyfinRepository, private val downloader: Downloader) :
    ViewModel() {
    private val _state = MutableStateFlow(ShowState())
    val state = _state.asStateFlow()

    lateinit var showId: UUID

    fun loadShow(showId: UUID) {
        this.showId = showId
        viewModelScope.launch {
            try {
                val show = repository.getShow(showId)
                val nextUp = getNextUp(showId)
                val seasons = repository.getSeasons(showId)
                val hasDownloads = hasDownloadedEpisodes(seasons)
                val actors = getActors(show)
                val director = getDirector(show)
                val writers = getWriters(show)
                _state.emit(
                    _state.value.copy(
                        show = show,
                        nextUp = nextUp,
                        seasons = seasons,
                        hasDownloads = hasDownloads,
                        actors = actors,
                        director = director,
                        writers = writers,
                    )
                )
            } catch (e: Exception) {
                _state.emit(_state.value.copy(error = e))
            }
        }
    }

    private suspend fun getNextUp(showId: UUID): FindroidEpisode? {
        val nextUpItems = repository.getNextUp(showId)
        return nextUpItems.getOrNull(0)
    }

    private suspend fun getActors(item: FindroidShow): List<FindroidItemPerson> {
        return withContext(Dispatchers.Default) {
            item.people.filter { it.type == PersonKind.ACTOR }
        }
    }

    private suspend fun getDirector(item: FindroidShow): FindroidItemPerson? {
        return withContext(Dispatchers.Default) {
            item.people.firstOrNull { it.type == PersonKind.DIRECTOR }
        }
    }

    private suspend fun getWriters(item: FindroidShow): List<FindroidItemPerson> {
        return withContext(Dispatchers.Default) {
            item.people.filter { it.type == PersonKind.WRITER }
        }
    }

    private suspend fun hasDownloadedEpisodes(seasons: List<FindroidSeason>): Boolean {
        for (season in seasons) {
            val episodes =
                runCatching {
                        repository.getEpisodes(
                            seriesId = showId,
                            seasonId = season.id,
                            offline = true,
                        )
                    }
                    .getOrElse { emptyList() }
            if (episodes.any { it.isDownloaded() }) {
                return true
            }
        }
        return false
    }

    fun downloadSeasons(seasonIds: Set<UUID>, storageIndex: Int) {
        if (seasonIds.isEmpty()) {
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val episodeIds = mutableSetOf<UUID>()
            for (seasonId in seasonIds) {
                val seasonEpisodes =
                    runCatching {
                            repository.getEpisodes(
                                seriesId = showId,
                                seasonId = seasonId,
                                fields = listOf(ItemFields.OVERVIEW),
                            )
                        }
                        .getOrElse { emptyList() }
                seasonEpisodes.forEach { episode -> episodeIds.add(episode.id) }
            }
            downloadEpisodes(episodeIds.toList(), storageIndex)
        }
    }

    fun deleteShowDownloads(seasonIds: List<UUID>) {
        if (seasonIds.isEmpty()) {
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            for (seasonId in seasonIds) {
                val episodes =
                    runCatching {
                            repository.getEpisodes(
                                seriesId = showId,
                                seasonId = seasonId,
                                offline = true,
                            )
                        }
                        .getOrElse { emptyList() }
                deleteEpisodes(episodes)
            }
            loadShow(showId)
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

    fun onAction(action: ShowAction) {
        when (action) {
            is ShowAction.MarkAsPlayed -> {
                viewModelScope.launch {
                    repository.markAsPlayed(showId)
                    loadShow(showId)
                }
            }
            is ShowAction.UnmarkAsPlayed -> {
                viewModelScope.launch {
                    repository.markAsUnplayed(showId)
                    loadShow(showId)
                }
            }
            is ShowAction.MarkAsFavorite -> {
                viewModelScope.launch {
                    repository.markAsFavorite(showId)
                    loadShow(showId)
                }
            }
            is ShowAction.UnmarkAsFavorite -> {
                viewModelScope.launch {
                    repository.unmarkAsFavorite(showId)
                    loadShow(showId)
                }
            }
            else -> Unit
        }
    }
}
