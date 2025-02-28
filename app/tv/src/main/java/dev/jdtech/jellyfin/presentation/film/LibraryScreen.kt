package dev.jdtech.jellyfin.presentation.film

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jdtech.jellyfin.core.presentation.dummy.dummyMovies
import dev.jdtech.jellyfin.film.presentation.library.LibraryState
import dev.jdtech.jellyfin.film.presentation.library.LibraryViewModel
import dev.jdtech.jellyfin.models.CollectionType
import dev.jdtech.jellyfin.models.FindroidFolder
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.FindroidMovie
import dev.jdtech.jellyfin.models.FindroidShow
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.ui.components.Direction
import dev.jdtech.jellyfin.ui.components.ItemCard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.util.UUID

@Composable
fun LibraryScreen(
    libraryId: UUID,
    libraryName: String,
    libraryType: CollectionType,
    navigateToLibrary: (libraryId: UUID, libraryName: String, libraryType: CollectionType) -> Unit,
    navigateToMovie: (itemId: UUID) -> Unit,
    navigateToShow: (itemId: UUID) -> Unit,
    libraryViewModel: LibraryViewModel = hiltViewModel(),
) {
    LaunchedEffect(true) {
        libraryViewModel.loadItems(libraryId, libraryType)
    }

    val state by libraryViewModel.state.collectAsState()

    LibraryScreenLayout(
        libraryName = libraryName,
        state = state,
        onClick = { item ->
            when (item) {
                is FindroidMovie -> navigateToMovie(item.id)
                is FindroidShow -> navigateToShow(item.id)
                is FindroidFolder -> navigateToLibrary(item.id, item.name, libraryType)
            }
        },
    )
}

@Composable
private fun LibraryScreenLayout(
    libraryName: String,
    state: LibraryState,
    onClick: (FindroidItem) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val items = state.items.collectAsLazyPagingItems()
    LazyVerticalGrid(
        columns = GridCells.Fixed(5),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.default),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.default),
        contentPadding = PaddingValues(horizontal = MaterialTheme.spacings.default * 2, vertical = MaterialTheme.spacings.large),
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester),
    ) {
        item(span = { GridItemSpan(this.maxLineSpan) }) {
            Text(
                text = libraryName,
                style = MaterialTheme.typography.displayMedium,
            )
        }
        items(items.itemCount) { i ->
            val item = items[i]
            item?.let {
                ItemCard(
                    item = item,
                    direction = Direction.VERTICAL,
                    onClick = {
                        onClick(item)
                    },
                )
            }
        }
    }

    LaunchedEffect(items.itemCount > 0) {
        if (items.itemCount > 0) {
            focusRequester.requestFocus()
        }
    }
}

@Preview(device = "id:tv_1080p")
@Composable
private fun LibraryScreenLayoutPreview() {
    val data: Flow<PagingData<FindroidItem>> = flowOf(PagingData.from(dummyMovies))
    FindroidTheme {
        LibraryScreenLayout(
            libraryName = "Movies",
            state = LibraryState(),
            onClick = {},
        )
    }
}
