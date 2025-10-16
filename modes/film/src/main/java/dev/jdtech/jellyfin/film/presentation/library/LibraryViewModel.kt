package dev.jdtech.jellyfin.film.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.models.CollectionType
import dev.jdtech.jellyfin.models.SortBy
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import androidx.paging.filter
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.SortOrder
import java.util.UUID
import javax.inject.Inject
import timber.log.Timber

@HiltViewModel
class LibraryViewModel
@Inject
constructor(
    private val jellyfinRepository: JellyfinRepository,
    private val appPreferences: AppPreferences,
) : ViewModel() {
    private val _state = MutableStateFlow(LibraryState())
    val state = _state.asStateFlow()

    lateinit var parentId: UUID
    lateinit var libraryType: CollectionType

    lateinit var sortBy: SortBy
    lateinit var sortOrder: SortOrder

    fun setup(
        parentId: UUID,
        libraryType: CollectionType,
    ) {
        this.parentId = parentId
        this.libraryType = libraryType
    }

    fun loadItems() {
        val itemType = when (libraryType) {
            CollectionType.Movies -> listOf(BaseItemKind.MOVIE)
            CollectionType.TvShows -> listOf(BaseItemKind.SERIES)
            CollectionType.BoxSets -> listOf(BaseItemKind.BOX_SET)
            CollectionType.Mixed -> listOf(BaseItemKind.FOLDER, BaseItemKind.MOVIE, BaseItemKind.SERIES)
            else -> null
        }

        val recursive = itemType == null || !itemType.contains(BaseItemKind.FOLDER)

        viewModelScope.launch {
            _state.emit(_state.value.copy(isLoading = true, error = null))

            initSorting()

            try {
                val items = jellyfinRepository.getItemsPaging(
                    parentId = parentId,
                    includeTypes = itemType,
                    recursive = recursive,
                    sortBy = if (libraryType == CollectionType.TvShows && sortBy == SortBy.DATE_PLAYED) SortBy.SERIES_DATE_PLAYED else sortBy, // Jellyfin uses a different enum for sorting series by data played
                    sortOrder = sortOrder,
                ).cachedIn(viewModelScope)
                // Extract genres from a non-paged fetch (small sample)
                val sample = jellyfinRepository.getItems(parentId = parentId, includeTypes = itemType, recursive = recursive, sortBy = sortBy, sortOrder = sortOrder, startIndex = 0, limit = 200)
                val genres = sample.flatMap {
                    when (it) {
                        is dev.jdtech.jellyfin.models.FindroidMovie -> it.genres
                        is dev.jdtech.jellyfin.models.FindroidShow -> it.genres
                        else -> emptyList()
                    }
                }.distinct().sorted()

                // If a genre is selected, apply client-side filtering on the paging flow
                val finalItems = if (_state.value.selectedGenre.isNullOrBlank()) {
                    items
                } else {
                    items.map { pagingData ->
                        pagingData.filter { item ->
                            when (item) {
                                is dev.jdtech.jellyfin.models.FindroidMovie -> item.genres.contains(_state.value.selectedGenre)
                                is dev.jdtech.jellyfin.models.FindroidShow -> item.genres.contains(_state.value.selectedGenre)
                                else -> false
                            }
                        }
                    }
                }

                // Diagnostic log: how many genres were discovered (show up to 10)
                Timber.d("LibraryViewModel: Loaded genres (count=%d): %s", genres.size, genres.take(10).joinToString(", "))
                _state.emit(_state.value.copy(items = finalItems, genres = genres))
            } catch (e: Exception) {
                _state.emit(_state.value.copy(error = e))
            }
        }
    }

    fun selectGenre(genre: String?) {
        viewModelScope.launch {
            // If clicking the already selected genre, deselect it (toggle behavior)
            val currentSelectedGenre = _state.value.selectedGenre
            val newSelectedGenre = if (genre == currentSelectedGenre) null else genre
            
            _state.emit(_state.value.copy(selectedGenre = newSelectedGenre))
            loadItems()
        }
    }

    private suspend fun initSorting() {
        if (!::sortBy.isInitialized || !::sortOrder.isInitialized) {
            sortBy = SortBy.fromString(appPreferences.getValue(appPreferences.sortBy))
            sortOrder = SortOrder.fromName(appPreferences.getValue(appPreferences.sortOrder))
            _state.emit(_state.value.copy(sortBy = sortBy, sortOrder = sortOrder))
        }
    }

    private fun setSorting(sortBy: SortBy, sortOrder: SortOrder) {
        this.sortBy = sortBy
        this.sortOrder = sortOrder
        viewModelScope.launch {
            _state.emit(_state.value.copy(sortBy = sortBy, sortOrder = sortOrder))
            appPreferences.setValue(appPreferences.sortBy, sortBy.toString())
            appPreferences.setValue(appPreferences.sortOrder, sortOrder.toString())
        }
    }

    fun onAction(action: LibraryAction) {
        when (action) {
            is LibraryAction.ChangeSorting -> {
                if (action.sortBy != this.sortBy || action.sortOrder != this.sortOrder) {
                    setSorting(sortBy = action.sortBy, sortOrder = action.sortOrder)
                    loadItems()
                }
            }
            is LibraryAction.SelectGenre -> {
                selectGenre(action.genre)
            }
            else -> Unit
        }
    }
}
