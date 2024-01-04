package dev.jdtech.jellyfin.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.tv.foundation.lazy.grid.TvGridCells
import androidx.tv.foundation.lazy.grid.TvGridItemSpan
import androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.jdtech.jellyfin.destinations.MovieScreenDestination
import dev.jdtech.jellyfin.destinations.ShowScreenDestination
import dev.jdtech.jellyfin.models.CollectionType
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.FindroidMovie
import dev.jdtech.jellyfin.models.FindroidShow
import dev.jdtech.jellyfin.ui.components.Direction
import dev.jdtech.jellyfin.ui.components.ItemCard
import dev.jdtech.jellyfin.ui.dummy.dummyMovies
import dev.jdtech.jellyfin.ui.theme.FindroidTheme
import dev.jdtech.jellyfin.ui.theme.spacings
import dev.jdtech.jellyfin.viewmodels.LibraryViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.util.UUID

@Destination
@Composable
fun LibraryScreen(
    navigator: DestinationsNavigator,
    libraryId: UUID,
    libraryName: String,
    libraryType: CollectionType,
    libraryViewModel: LibraryViewModel = hiltViewModel(),
) {
    LaunchedEffect(true) {
        libraryViewModel.loadItems(libraryId, libraryType)
    }

    val delegatedUiState by libraryViewModel.uiState.collectAsState()

    LibraryScreenLayout(
        libraryName = libraryName,
        uiState = delegatedUiState,
        onClick = { item ->
            when (item) {
                is FindroidMovie -> {
                    navigator.navigate(MovieScreenDestination(item.id))
                }
                is FindroidShow -> {
                    navigator.navigate(ShowScreenDestination(item.id))
                }
            }
        },
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun LibraryScreenLayout(
    libraryName: String,
    uiState: LibraryViewModel.UiState,
    onClick: (FindroidItem) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }

    when (uiState) {
        is LibraryViewModel.UiState.Loading -> Text(text = "LOADING")
        is LibraryViewModel.UiState.Normal -> {
            val items = uiState.items.collectAsLazyPagingItems()
            TvLazyVerticalGrid(
                columns = TvGridCells.Fixed(5),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.default),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.default),
                contentPadding = PaddingValues(horizontal = MaterialTheme.spacings.default * 2, vertical = MaterialTheme.spacings.large),
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(focusRequester),
            ) {
                item(span = { TvGridItemSpan(this.maxLineSpan) }) {
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
        is LibraryViewModel.UiState.Error -> Text(text = uiState.error.toString())
    }
}

@Preview(device = "id:tv_1080p")
@Composable
private fun LibraryScreenLayoutPreview() {
    val data: Flow<PagingData<FindroidItem>> = flowOf(PagingData.from(dummyMovies))
    FindroidTheme {
        LibraryScreenLayout(
            libraryName = "Movies",
            uiState = LibraryViewModel.UiState.Normal(data),
            onClick = {},
        )
    }
}
