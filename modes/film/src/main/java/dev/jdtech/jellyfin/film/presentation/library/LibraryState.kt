package dev.jdtech.jellyfin.film.presentation.library

import androidx.paging.PagingData
import dev.jdtech.jellyfin.models.JellyCastItem
import dev.jdtech.jellyfin.models.SortBy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.jellyfin.sdk.model.api.SortOrder

data class LibraryState(
    val items: Flow<PagingData<JellyCastItem>> = emptyFlow(),
    val sortBy: SortBy = SortBy.NAME,
    val sortOrder: SortOrder = SortOrder.ASCENDING,
    val genres: List<String> = emptyList(),
    val selectedGenre: String? = null,
    val isLoading: Boolean = false,
    val error: Exception? = null,
)
