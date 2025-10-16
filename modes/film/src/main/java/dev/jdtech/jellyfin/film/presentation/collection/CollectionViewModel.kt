package dev.jdtech.jellyfin.film.presentation.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.core.Constants
import dev.jdtech.jellyfin.models.CollectionSection
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidMovie
import dev.jdtech.jellyfin.models.FindroidShow
import dev.jdtech.jellyfin.models.SortBy
import dev.jdtech.jellyfin.models.UiText
import dev.jdtech.jellyfin.repository.JellyfinRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import dev.jdtech.jellyfin.core.R as CoreR
import kotlinx.coroutines.flow.update
import timber.log.Timber

@HiltViewModel
class CollectionViewModel
@Inject
constructor(
    private val repository: JellyfinRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(CollectionState())
    val state = _state.asStateFlow()

    fun loadItems(parentId: UUID, onePerGenre: Boolean = false) {
        viewModelScope.launch {
            _state.emit(_state.value.copy(isLoading = true, error = null))

            try {
                var items = repository.getItems(
                    parentId = parentId,
                    sortBy = SortBy.RELEASE_DATE,
                )

                if (onePerGenre) {
                    // For movies and shows, keep the first item for each genre encountered.
                    val seenGenres = mutableSetOf<String>()
                    items = items.filter { item ->
                        val itemGenres = when (item) {
                            is FindroidMovie -> item.genres
                            is FindroidShow -> item.genres
                            else -> emptyList()
                        }
                        // If item has no genre, include it but only once
                        if (itemGenres.isEmpty()) {
                            // treat empty genre as a special key
                            val key = "__no_genre__"
                            if (seenGenres.contains(key)) {
                                false
                            } else {
                                seenGenres.add(key)
                                true
                            }
                        } else {
                            // include item if at least one of its genres is not yet seen
                            val newGenre = itemGenres.firstOrNull { g -> !seenGenres.contains(g) }
                            if (newGenre != null) {
                                seenGenres.add(newGenre)
                                true
                            } else {
                                false
                            }
                        }
                    }
                }

                val sections = mutableListOf<CollectionSection>()

                // extract genres from all items
                val genres = items.flatMap {
                    when (it) {
                        is FindroidMovie -> it.genres
                        is FindroidShow -> it.genres
                        else -> emptyList()
                    }
                }.distinct().sorted()

                // Diagnostic log: how many genres were discovered (show up to 10)
                Timber.d("CollectionViewModel: Loaded genres (count=%d): %s", genres.size, genres.take(10).joinToString(", "))

                withContext(Dispatchers.Default) {
                    CollectionSection(
                        Constants.FAVORITE_TYPE_MOVIES,
                        UiText.StringResource(CoreR.string.movies_label),
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
                        UiText.StringResource(CoreR.string.shows_label),
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
                        UiText.StringResource(CoreR.string.episodes_label),
                        items.filterIsInstance<FindroidEpisode>(),
                    ).let {
                        if (it.items.isNotEmpty()) {
                            sections.add(
                                it,
                            )
                        }
                    }
                }

                _state.emit(_state.value.copy(isLoading = false, sections = sections, genres = genres))
            } catch (e: Exception) {
                _state.emit(_state.value.copy(isLoading = false, error = e))
            }
        }
    }

    fun selectGenre(genre: String?) {
        viewModelScope.launch {
            // Filter current sections client-side by genre
            val allSections = _state.value.sections
            val filteredSections = if (genre.isNullOrBlank()) {
                allSections
            } else {
                allSections.map { section ->
                    section.copy(items = section.items.filter { item ->
                        when (item) {
                            is FindroidMovie -> item.genres.contains(genre)
                            is FindroidShow -> item.genres.contains(genre)
                            else -> false
                        }
                    })
                }.filter { it.items.isNotEmpty() }
            }

            _state.update { it.copy(selectedGenre = genre, sections = filteredSections) }
        }
    }
}
