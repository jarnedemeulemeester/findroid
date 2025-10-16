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
        data class Normal(
            val sections: List<CollectionSection>,
            val items: List<FindroidItem>,
            val genres: List<String> = emptyList(),
            val selectedGenre: String? = null
        ) : UiState()
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
            
            // Extract genres from all items
            val genres = items.flatMap {
                when (it) {
                    is FindroidMovie -> it.genres
                    is FindroidShow -> it.genres
                    else -> emptyList()
                }
            }.distinct().sorted()
            
            Timber.tag("DownloadsVM").d("Built %d sections with total %d items, %d genres", sections.size, items.size, genres.size)
            _uiState.emit(UiState.Normal(sections, items, genres, null))
        }
    }
    
    fun selectGenre(genre: String?) {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is UiState.Normal) {
                // If clicking the already selected genre, deselect it (toggle behavior)
                val currentSelectedGenre = currentState.selectedGenre
                val newSelectedGenre = if (genre == currentSelectedGenre) null else genre
                
                // Filter sections by selected genre
                val filteredSections = if (newSelectedGenre == null) {
                    // Show all items
                    val sections = mutableListOf<CollectionSection>()
                    CollectionSection(
                        Constants.FAVORITE_TYPE_MOVIES,
                        UiText.StringResource(R.string.movies_label),
                        currentState.items.filterIsInstance<FindroidMovie>(),
                    ).let { if (it.items.isNotEmpty()) sections.add(it) }
                    
                    CollectionSection(
                        Constants.FAVORITE_TYPE_SHOWS,
                        UiText.StringResource(R.string.shows_label),
                        currentState.items.filterIsInstance<FindroidShow>(),
                    ).let { if (it.items.isNotEmpty()) sections.add(it) }
                    
                    CollectionSection(
                        Constants.FAVORITE_TYPE_EPISODES,
                        UiText.StringResource(R.string.episodes_label),
                        currentState.items.filterIsInstance<FindroidEpisode>(),
                    ).let { if (it.items.isNotEmpty()) sections.add(it) }
                    sections
                } else {
                    // Filter by genre
                    val sections = mutableListOf<CollectionSection>()
                    CollectionSection(
                        Constants.FAVORITE_TYPE_MOVIES,
                        UiText.StringResource(R.string.movies_label),
                        currentState.items.filterIsInstance<FindroidMovie>()
                            .filter { it.genres.contains(newSelectedGenre) },
                    ).let { if (it.items.isNotEmpty()) sections.add(it) }
                    
                    CollectionSection(
                        Constants.FAVORITE_TYPE_SHOWS,
                        UiText.StringResource(R.string.shows_label),
                        currentState.items.filterIsInstance<FindroidShow>()
                            .filter { it.genres.contains(newSelectedGenre) },
                    ).let { if (it.items.isNotEmpty()) sections.add(it) }
                    
                    CollectionSection(
                        Constants.FAVORITE_TYPE_EPISODES,
                        UiText.StringResource(R.string.episodes_label),
                        currentState.items.filterIsInstance<FindroidEpisode>(),
                    ).let { if (it.items.isNotEmpty()) sections.add(it) }
                    sections
                }
                
                _uiState.emit(UiState.Normal(filteredSections, currentState.items, currentState.genres, newSelectedGenre))
            }
        }
    }
}

sealed interface DownloadsEvent {
    data class ConnectionError(val error: Exception) : DownloadsEvent
}
