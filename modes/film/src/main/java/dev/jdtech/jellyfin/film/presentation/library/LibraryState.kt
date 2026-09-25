package dev.jdtech.jellyfin.film.presentation.library

import androidx.paging.PagingData
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.SortOrder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

data class LibraryState(
    val items: Flow<PagingData<FindroidItem>> = emptyFlow(),
    val sortBy: LibrarySortBy = LibrarySortBy.NAME,
    val sortOrder: SortOrder = SortOrder.ASCENDING,
    val isLoading: Boolean = false,
    val error: Exception? = null,
)
