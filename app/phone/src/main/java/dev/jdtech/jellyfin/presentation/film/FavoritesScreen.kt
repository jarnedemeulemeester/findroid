package dev.jdtech.jellyfin.presentation.film

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jdtech.jellyfin.core.presentation.dummy.dummyMovies
import dev.jdtech.jellyfin.film.presentation.collection.CollectionAction
import dev.jdtech.jellyfin.film.presentation.collection.CollectionState
import dev.jdtech.jellyfin.film.presentation.favorites.FavoritesViewModel
import dev.jdtech.jellyfin.models.CollectionSection
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.UiText
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.core.R as CoreR

@Composable
fun FavoritesScreen(
    onItemClick: (item: FindroidItem) -> Unit,
    navigateBack: () -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(true) {
        viewModel.loadItems()
    }

    CollectionScreenLayout(
        collectionName = stringResource(CoreR.string.title_favorite),
        state = state,
        onBack = { navigateBack() },
        onItemClick = { item -> onItemClick(item) },
        onGenreSelected = { /* no-op for favorites */ },
        onePerGenreState = false,
        onToggleOnePerGenre = {},
    )
}

@PreviewScreenSizes
@Composable
private fun CollectionScreenLayoutPreview() {
    FindroidTheme {
        CollectionScreenLayout(
            collectionName = "Favorites",
            state = CollectionState(
                sections = listOf(
                    CollectionSection(
                        id = 0,
                        name = UiText.StringResource(CoreR.string.title_favorite),
                        items = dummyMovies,
                    ),
                ),
            ),
                onBack = {},
                onItemClick = {},
                onGenreSelected = {},
                onePerGenreState = false,
                onToggleOnePerGenre = {},
        )
    }
}
