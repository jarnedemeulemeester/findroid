package dev.jdtech.jellyfin.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.core.Constants
import dev.jdtech.jellyfin.core.R
import dev.jdtech.jellyfin.models.CollectionSection
import dev.jdtech.jellyfin.models.FindroidMovie
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidShow
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.UiText
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import timber.log.Timber

@HiltViewModel
class DownloadsViewModel
@Inject
constructor(
    private val appPreferences: AppPreferences,
    private val repository: JellyfinRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val eventsChannel = Channel<DownloadsEvent>()
    val eventsChannelFlow = eventsChannel.receiveAsFlow()

    sealed class UiState {
        data class Normal(val sections: List<CollectionSection>, val items: List<FindroidItem>) : UiState()
        data object Loading : UiState()
        data class Error(val error: Exception) : UiState()
    }

    init {
        Timber.tag("DownloadsVM").d("ViewModel CREATED - instance hashCode=${this.hashCode()}")
        testServerConnection()
        loadData() // Always load data when ViewModel is created
    }

    private fun testServerConnection() {
        viewModelScope.launch {
            try {
                if (appPreferences.getValue(appPreferences.offlineMode)) return@launch
                repository.getPublicSystemInfo()
                // Give the UI a chance to load
                delay(100)
            } catch (e: Exception) {
                eventsChannel.send(DownloadsEvent.ConnectionError(e))
            }
        }
    }

    fun loadData() {
        viewModelScope.launch {
            Timber.tag("DownloadsVM").d("loadData() called -> emit Loading")
            _uiState.emit(UiState.Loading)

            val sections = mutableListOf<CollectionSection>()

            Timber.tag("DownloadsVM").d("Fetching downloads from repository…")
            val items = repository.getDownloads()
            Timber.tag("DownloadsVM").d(
                "Repository returned %d items (movies=%d, shows=%d, episodes=%d)",
                items.size,
                items.count { it is FindroidMovie },
                items.count { it is FindroidShow },
                items.count { it is FindroidEpisode },
            )

            CollectionSection(
                Constants.FAVORITE_TYPE_MOVIES,
                UiText.StringResource(R.string.movies_label),
                items.filterIsInstance<FindroidMovie>(),
            ).let {
                if (it.items.isNotEmpty()) {
                    sections.add(
                        it,
                    )
                }
            }
            CollectionSection(
                Constants.FAVORITE_TYPE_SHOWS,
                UiText.StringResource(R.string.shows_label),
                items.filterIsInstance<FindroidShow>(),
            ).let {
                if (it.items.isNotEmpty()) {
                    sections.add(
                        it,
                    )
                }
            }
            CollectionSection(
                Constants.FAVORITE_TYPE_EPISODES,
                UiText.StringResource(R.string.episodes_label),
                items.filterIsInstance<FindroidEpisode>(),
            ).let {
                if (it.items.isNotEmpty()) {
                    sections.add(
                        it,
                    )
                }
            }
            Timber.tag("DownloadsVM").d("Built %d sections with total %d items", sections.size, items.size)
            _uiState.emit(UiState.Normal(sections, items))
        }
    }
}

sealed interface DownloadsEvent {
    data class ConnectionError(val error: Exception) : DownloadsEvent
}
