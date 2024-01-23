package dev.jdtech.jellyfin.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.models.EpisodeItem
import dev.jdtech.jellyfin.models.FindroidSeason
import dev.jdtech.jellyfin.repository.JellyfinRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.ItemFields
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SeasonViewModel
@Inject
constructor(
    private val jellyfinRepository: JellyfinRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val eventsChannel = Channel<SeasonEvent>()
    val eventsChannelFlow = eventsChannel.receiveAsFlow()

    sealed class UiState {
        data class Normal(val episodes: List<EpisodeItem>) : UiState()
        data object Loading : UiState()
        data class Error(val error: Exception) : UiState()
    }

    private lateinit var season: FindroidSeason

    fun loadEpisodes(seriesId: UUID, seasonId: UUID, offline: Boolean) {
        viewModelScope.launch {
            _uiState.emit(UiState.Loading)
            try {
                season = getSeason(seasonId)
                val episodes = getEpisodes(seriesId, seasonId, offline)
                _uiState.emit(UiState.Normal(episodes))
            } catch (_: NullPointerException) {
                // Navigate back because item does not exist (probably because it's been deleted)
                eventsChannel.send(SeasonEvent.NavigateBack)
            } catch (e: Exception) {
                _uiState.emit(UiState.Error(e))
            }
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

sealed interface SeasonEvent {
    data object NavigateBack : SeasonEvent
}
