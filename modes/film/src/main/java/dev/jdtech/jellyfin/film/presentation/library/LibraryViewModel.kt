package dev.jdtech.jellyfin.film.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.models.CollectionType
import dev.jdtech.jellyfin.models.SortBy
import dev.jdtech.jellyfin.models.SortOrder
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.BaseItemKind

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

    lateinit var sortBy: LibrarySortBy
    lateinit var sortOrder: SortOrder

    fun setup(parentId: UUID, libraryType: CollectionType) {
        this.parentId = parentId
        this.libraryType = libraryType
    }

    fun loadItems() {
        viewModelScope.launch {
            _state.emit(_state.value.copy(isLoading = true, error = null))

            initSorting()

            val itemType =
                when (libraryType) {
                    CollectionType.Movies -> listOf(BaseItemKind.MOVIE)
                    CollectionType.TvShows ->
                        if (sortBy.displaysEpisodes) {
                            listOf(BaseItemKind.EPISODE)
                        } else {
                            listOf(BaseItemKind.SERIES)
                        }
                    CollectionType.BoxSets -> listOf(BaseItemKind.BOX_SET)
                    CollectionType.Mixed,
                    CollectionType.Folders ->
                        listOf(BaseItemKind.FOLDER, BaseItemKind.MOVIE, BaseItemKind.SERIES)
                    else -> null
                }

            val recursive = itemType == null || !itemType.contains(BaseItemKind.FOLDER)
            val repositorySortBy =
                if (
                    libraryType == CollectionType.TvShows &&
                        sortBy == LibrarySortBy.DATE_PLAYED
                ) {
                    SortBy.SERIES_DATE_PLAYED
                } else {
                    sortBy.repositorySortBy
                }

            try {
                val items =
                    jellyfinRepository
                        .getItemsPaging(
                            parentId = parentId,
                            includeTypes = itemType,
                            recursive = recursive,
                            sortBy = repositorySortBy,
                            sortOrder = sortOrder,
                        )
                        .cachedIn(viewModelScope)
                _state.emit(_state.value.copy(items = items))
            } catch (e: Exception) {
                _state.emit(_state.value.copy(error = e))
            }
        }
    }

    private suspend fun initSorting() {
        if (::sortBy.isInitialized && ::sortOrder.isInitialized) return

        val sortByPreference = appPreferences.librarySortBy(parentId)
        val sortOrderPreference = appPreferences.librarySortOrder(parentId)
        val savedSortBy = appPreferences.getValue(sortByPreference)
        val savedSortOrder = appPreferences.getValue(sortOrderPreference)

        sortBy =
            LibrarySortBy.fromString(
                savedSortBy ?: appPreferences.getValue(appPreferences.sortBy)
            )
        if (sortBy.displaysEpisodes && libraryType != CollectionType.TvShows) {
            sortBy = LibrarySortBy.defaultValue
        }
        sortOrder =
            SortOrder.fromString(
                savedSortOrder ?: appPreferences.getValue(appPreferences.sortOrder)
            )

        _state.emit(_state.value.copy(sortBy = sortBy, sortOrder = sortOrder))

        if (savedSortBy == null) {
            appPreferences.setValue(sortByPreference, sortBy.toString())
        }
        if (savedSortOrder == null) {
            appPreferences.setValue(sortOrderPreference, sortOrder.toString())
        }
    }

    private fun setSorting(sortBy: LibrarySortBy, sortOrder: SortOrder) {
        this.sortBy = sortBy
        this.sortOrder = sortOrder
        viewModelScope.launch {
            _state.emit(_state.value.copy(sortBy = sortBy, sortOrder = sortOrder))
            appPreferences.setValue(appPreferences.librarySortBy(parentId), sortBy.toString())
            appPreferences.setValue(appPreferences.librarySortOrder(parentId), sortOrder.toString())
        }
    }

    fun onAction(action: LibraryAction) {
        when (action) {
            is LibraryAction.ChangeSorting -> {
                if (action.sortBy != sortBy || action.sortOrder != sortOrder) {
                    setSorting(sortBy = action.sortBy, sortOrder = action.sortOrder)
                    loadItems()
                }
            }
            else -> Unit
        }
    }
}
