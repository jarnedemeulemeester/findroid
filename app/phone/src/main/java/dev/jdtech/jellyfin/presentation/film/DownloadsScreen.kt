package dev.jdtech.jellyfin.presentation.film

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jdtech.jellyfin.core.presentation.dummy.dummyMovies
import dev.jdtech.jellyfin.film.presentation.collection.CollectionAction
import dev.jdtech.jellyfin.film.presentation.collection.CollectionState
import dev.jdtech.jellyfin.film.presentation.downloads.DownloadsViewModel
import dev.jdtech.jellyfin.models.CollectionSection
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.UiText
import dev.jdtech.jellyfin.presentation.film.components.Direction
import dev.jdtech.jellyfin.presentation.film.components.ItemCard
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.presentation.utils.GridCellsAdaptiveWithMinColumns
import dev.jdtech.jellyfin.presentation.utils.rememberSafePadding
import dev.jdtech.jellyfin.core.R as CoreR

@Composable
fun DownloadsScreen(
    onItemClick: (item: FindroidItem) -> Unit,
    viewModel: DownloadsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(true) {
        viewModel.loadItems()
    }

    DownloadsScreenLayout(
        state = state,
        onAction = { action ->
            when (action) {
                is CollectionAction.OnItemClick -> onItemClick(action.item)
                is CollectionAction.OnBackClick -> Unit
            }
        },
    )
}

@Composable
private fun DownloadsScreenLayout(
    state: CollectionState,
    onAction: (CollectionAction) -> Unit,
) {
    val safePadding = rememberSafePadding(
        handleStartInsets = false,
    )

    LazyVerticalGrid(
        columns = GridCellsAdaptiveWithMinColumns(minSize = 160.dp, minColumns = 2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = safePadding.start + MaterialTheme.spacings.default,
            top = safePadding.top + MaterialTheme.spacings.small,
            end = safePadding.end + MaterialTheme.spacings.default,
            bottom = safePadding.bottom + MaterialTheme.spacings.default,
        ),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.default),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.default),
    ) {
        state.sections.forEach { section ->
            stickyHeader {
                Card {
                    Text(
                        text = section.name.asString(),
                        modifier = Modifier
                            .padding(
                                horizontal = MaterialTheme.spacings.medium,
                                vertical = MaterialTheme.spacings.medium,
                            ),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
            items(
                items = section.items,
                key = { it.id },
            ) { item ->
                ItemCard(
                    item = item,
                    direction = Direction.VERTICAL,
                    onClick = {
                        onAction(CollectionAction.OnItemClick(item))
                    },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

@PreviewScreenSizes
@Composable
private fun DownloadsScreenLayoutPreview() {
    FindroidTheme {
        DownloadsScreenLayout(
            state = CollectionState(
                sections = listOf(
                    CollectionSection(
                        id = 0,
                        name = UiText.StringResource(CoreR.string.movies_label),
                        items = dummyMovies,
                    ),
                ),
            ),
            onAction = {},
        )
    }
}
