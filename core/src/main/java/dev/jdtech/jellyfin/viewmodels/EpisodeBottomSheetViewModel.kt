package dev.jdtech.jellyfin.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.repository.JellyfinRepository
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jellyfin.sdk.api.client.exception.ApiClientException
import timber.log.Timber

@HiltViewModel
class EpisodeBottomSheetViewModel
@Inject
constructor(
    private val jellyfinRepository: JellyfinRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    sealed class UiState {
        data class Normal(
            val episode: FindroidEpisode,
        ) : UiState()

        object Loading : UiState()
        data class Error(val error: Exception) : UiState()
    }

    lateinit var item: FindroidEpisode

    fun loadEpisode(episodeId: UUID) {
        viewModelScope.launch {
            _uiState.emit(UiState.Loading)
            try {
                item = jellyfinRepository.getEpisode(episodeId)
                _uiState.emit(
                    UiState.Normal(
                        item,
                    )
                )
            } catch (e: Exception) {
                _uiState.emit(UiState.Error(e))
            }
        }
    }

    fun togglePlayed() {
        viewModelScope.launch {
            try {
                if (item.played) {
                    jellyfinRepository.markAsUnplayed(item.id)
                } else {
                    jellyfinRepository.markAsPlayed(item.id)
                }
                loadEpisode(item.id)
            } catch (e: ApiClientException) {
                Timber.d(e)
            }
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            try {
                if (item.favorite) {
                    jellyfinRepository.unmarkAsFavorite(item.id)
                } else {
                    jellyfinRepository.markAsFavorite(item.id)
                }
                loadEpisode(item.id)
            } catch (e: ApiClientException) {
                Timber.d(e)
            }
        }
    }

    fun download() {
    }

    fun deleteEpisode() {
    }
}
