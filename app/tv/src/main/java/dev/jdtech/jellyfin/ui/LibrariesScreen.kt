package dev.jdtech.jellyfin.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.foundation.lazy.grid.TvGridCells
import androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid
import androidx.tv.foundation.lazy.grid.items
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.jdtech.jellyfin.models.CollectionType
import dev.jdtech.jellyfin.ui.components.Direction
import dev.jdtech.jellyfin.ui.components.ItemCard
import dev.jdtech.jellyfin.ui.destinations.LibraryScreenDestination
import dev.jdtech.jellyfin.ui.dummy.dummyCollections
import dev.jdtech.jellyfin.ui.theme.FindroidTheme
import dev.jdtech.jellyfin.ui.theme.spacings
import dev.jdtech.jellyfin.viewmodels.MediaViewModel
import java.util.UUID

@Destination
@Composable
fun LibrariesScreen(
    navigator: DestinationsNavigator,
    mediaViewModel: MediaViewModel = hiltViewModel(),
) {
    val delegatedUiState by mediaViewModel.uiState.collectAsState()

    LibrariesScreenLayout(
        uiState = delegatedUiState,
        onClick = { libraryId, libraryName, libraryType ->
            navigator.navigate(LibraryScreenDestination(libraryId, libraryName, libraryType))
        },
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun LibrariesScreenLayout(
    uiState: MediaViewModel.UiState,
    onClick: (UUID, String, CollectionType) -> Unit,
) {
    when (uiState) {
        is MediaViewModel.UiState.Loading -> Text(text = "LOADING")
        is MediaViewModel.UiState.Normal -> {
            TvLazyVerticalGrid(
                columns = TvGridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.large),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.large),
                contentPadding = PaddingValues(
                    start = MaterialTheme.spacings.large,
                    top = MaterialTheme.spacings.small,
                    end = MaterialTheme.spacings.large,
                    bottom = MaterialTheme.spacings.large,
                ),
            ) {
                items(uiState.collections) { collection ->
                    ItemCard(
                        item = collection,
                        direction = Direction.HORIZONTAL,
                        onClick = {
                            onClick(collection.id, collection.name, collection.type)
                        },
                    )
                }
            }
        }
        is MediaViewModel.UiState.Error -> Text(text = uiState.error.toString())
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Preview(widthDp = 960, heightDp = 540)
@Composable
private fun LibrariesScreenLayoutPreview() {
    FindroidTheme {
        Surface {
            LibrariesScreenLayout(
                uiState = MediaViewModel.UiState.Normal(dummyCollections),
                onClick = { _, _, _ -> },
            )
        }
    }
}
