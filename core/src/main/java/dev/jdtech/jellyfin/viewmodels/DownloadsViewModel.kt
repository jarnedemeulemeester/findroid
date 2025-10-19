package dev.jdtech.jellyfin.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.core.Constants
import dev.jdtech.jellyfin.core.R
import dev.jdtech.jellyfin.models.CollectionSection
import dev.jdtech.jellyfin.models.JellyCastMovie
import dev.jdtech.jellyfin.models.JellyCastEpisode
import dev.jdtech.jellyfin.models.JellyCastShow
import dev.jdtech.jellyfin.models.JellyCastItem
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
            val items: List<JellyCastItem>,
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
            val allItems = repository.getDownloads()
            // Filter only items that have at least one LOCAL source
            val items = allItems.filter { item ->
                item.sources.any { it.type == dev.jdtech.jellyfin.models.JellyCastSourceType.LOCAL }
            }
            Timber.tag("DownloadsVM").d(
                "Repository returned %d items, filtered to %d with LOCAL sources (movies=%d, shows=%d, episodes=%d)",
                allItems.size,
                items.size,
                items.count { it is JellyCastMovie },
                items.count { it is JellyCastShow },
                items.count { it is JellyCastEpisode },
            )

            CollectionSection(
                Constants.FAVORITE_TYPE_MOVIES,
                UiText.StringResource(R.string.movies_label),
                items.filterIsInstance<JellyCastMovie>(),
            ).let {
                if (it.items.isNotEmpty()) {
                    sections.add(
                        it,
                    )
                }
            }
            
            // Create virtual shows from downloaded episodes grouped by series
            val episodesDownloaded = items.filterIsInstance<JellyCastEpisode>()
            val showsFromEpisodes = episodesDownloaded
                .groupBy { it.seriesId }
                .mapNotNull { (seriesId, episodes) ->
                    // Get the first episode to extract series info
                    val firstEpisode = episodes.firstOrNull() ?: return@mapNotNull null
                    
                    // Create a virtual JellyCastShow with episode count
                    JellyCastShow(
                        id = seriesId,
                        name = firstEpisode.seriesName,
                        originalTitle = firstEpisode.seriesName,
                        overview = "${episodes.size} episodio${if (episodes.size != 1) "s" else ""} descargado${if (episodes.size != 1) "s" else ""}",
                        sources = emptyList(),
                        seasons = emptyList(),
                        played = episodes.all { it.played },
                        favorite = false,
                        canPlay = true,
                        canDownload = false,
                        playbackPositionTicks = 0L,
                        unplayedItemCount = episodes.count { !it.played },
                        genres = emptyList(),
                        people = emptyList(),
                        runtimeTicks = episodes.sumOf { it.runtimeTicks },
                        communityRating = firstEpisode.communityRating,
                        officialRating = "",
                        status = "",
                        productionYear = null,
                        endDate = null,
                        trailer = null,
                        images = firstEpisode.images,
                        chapters = emptyList()
                    )
                }
            
            CollectionSection(
                Constants.FAVORITE_TYPE_SHOWS,
                UiText.StringResource(R.string.shows_label),
                (items.filterIsInstance<JellyCastShow>() + showsFromEpisodes).distinctBy { it.id },
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
                episodesDownloaded,
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
                    is JellyCastMovie -> it.genres
                    is JellyCastShow -> it.genres
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
                        currentState.items.filterIsInstance<JellyCastMovie>(),
                    ).let { if (it.items.isNotEmpty()) sections.add(it) }
                    
                    CollectionSection(
                        Constants.FAVORITE_TYPE_SHOWS,
                        UiText.StringResource(R.string.shows_label),
                        currentState.items.filterIsInstance<JellyCastShow>(),
                    ).let { if (it.items.isNotEmpty()) sections.add(it) }
                    
                    CollectionSection(
                        Constants.FAVORITE_TYPE_EPISODES,
                        UiText.StringResource(R.string.episodes_label),
                        currentState.items.filterIsInstance<JellyCastEpisode>(),
                    ).let { if (it.items.isNotEmpty()) sections.add(it) }
                    sections
                } else {
                    // Filter by genre
                    val sections = mutableListOf<CollectionSection>()
                    CollectionSection(
                        Constants.FAVORITE_TYPE_MOVIES,
                        UiText.StringResource(R.string.movies_label),
                        currentState.items.filterIsInstance<JellyCastMovie>()
                            .filter { it.genres.contains(newSelectedGenre) },
                    ).let { if (it.items.isNotEmpty()) sections.add(it) }
                    
                    CollectionSection(
                        Constants.FAVORITE_TYPE_SHOWS,
                        UiText.StringResource(R.string.shows_label),
                        currentState.items.filterIsInstance<JellyCastShow>()
                            .filter { it.genres.contains(newSelectedGenre) },
                    ).let { if (it.items.isNotEmpty()) sections.add(it) }
                    
                    CollectionSection(
                        Constants.FAVORITE_TYPE_EPISODES,
                        UiText.StringResource(R.string.episodes_label),
                        currentState.items.filterIsInstance<JellyCastEpisode>(),
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
