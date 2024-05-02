package dev.jdtech.jellyfin.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.foundation.lazy.grid.TvGridCells
import androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid
import androidx.tv.foundation.lazy.grid.items
import androidx.tv.material3.MaterialTheme
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.jdtech.jellyfin.destinations.LibraryScreenDestination
import dev.jdtech.jellyfin.models.CollectionType
import dev.jdtech.jellyfin.models.FindroidCollection
import dev.jdtech.jellyfin.ui.components.Direction
import dev.jdtech.jellyfin.ui.components.ItemCard
import dev.jdtech.jellyfin.ui.dummy.dummyCollections
import dev.jdtech.jellyfin.ui.theme.FindroidTheme
import dev.jdtech.jellyfin.ui.theme.spacings
import dev.jdtech.jellyfin.viewmodels.MediaViewModel
import java.util.UUID

@Destination
@Composable
fun LibrariesScreen(
    navigator: DestinationsNavigator,
    isLoading: (Boolean) -> Unit,
    mediaViewModel: MediaViewModel = hiltViewModel(),
) {
    val delegatedUiState by mediaViewModel.uiState.collectAsState()

    LibrariesScreenLayout(
        uiState = delegatedUiState,
        isLoading = isLoading,
        onClick = { libraryId, libraryName, libraryType ->
            navigator.navigate(LibraryScreenDestination(libraryId, libraryName, libraryType))
        },
    )
}

@Composable
private fun LibrariesScreenLayout(
    uiState: MediaViewModel.UiState,
    isLoading: (Boolean) -> Unit,
    onClick: (UUID, String, CollectionType) -> Unit,
) {
    var collections: List<FindroidCollection> by remember {
        mutableStateOf(emptyList())
    }

    when (uiState) {
        is MediaViewModel.UiState.Normal -> {
            collections = uiState.collections
            isLoading(false)
        }
        is MediaViewModel.UiState.Loading -> {
            isLoading(true)
        }
        else -> Unit
    }

    val focusRequester = remember { FocusRequester() }

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
        modifier = Modifier.focusRequester(focusRequester),
    ) {
        items(collections, key = { it.id }) { collection ->
            ItemCard(
                item = collection,
                direction = Direction.HORIZONTAL,
                onClick = {
                    onClick(collection.id, collection.name, collection.type)
                },
            )
        }
    }
    LaunchedEffect(collections) {
        focusRequester.requestFocus()
    }
}

@Preview(device = "id:tv_1080p")
@Composable
private fun LibrariesScreenLayoutPreview() {
    FindroidTheme {
        LibrariesScreenLayout(
            uiState = MediaViewModel.UiState.Normal(dummyCollections),
            isLoading = {},
            onClick = { _, _, _ -> },
        )
    }
}
