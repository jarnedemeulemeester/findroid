package dev.jdtech.jellyfin.presentation.film

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowWidthSizeClass
import dev.jdtech.jellyfin.core.presentation.dummy.dummyCollections
import dev.jdtech.jellyfin.film.presentation.media.MediaAction
import dev.jdtech.jellyfin.film.presentation.media.MediaState
import dev.jdtech.jellyfin.film.presentation.media.MediaViewModel
import dev.jdtech.jellyfin.models.FindroidCollection
import dev.jdtech.jellyfin.presentation.components.ErrorDialog
import dev.jdtech.jellyfin.presentation.film.components.Direction
import dev.jdtech.jellyfin.presentation.film.components.ErrorCard
import dev.jdtech.jellyfin.presentation.film.components.FavoritesCard
import dev.jdtech.jellyfin.presentation.film.components.FilmSearchBar
import dev.jdtech.jellyfin.presentation.film.components.ItemCard
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.presentation.utils.rememberSafePadding

@Composable
fun MediaScreen(
    onItemClick: (FindroidCollection) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: MediaViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(true) {
        viewModel.loadData()
    }

    MediaScreenLayout(
        state = state,
        onAction = { action ->
            when (action) {
                is MediaAction.OnItemClick -> onItemClick(action.item)
                is MediaAction.OnSettingsClick -> onSettingsClick()
                else -> Unit
            }
            viewModel.onAction(action)
        },
    )
}

@Composable
private fun MediaScreenLayout(
    state: MediaState,
    onAction: (MediaAction) -> Unit,
) {
    val safePadding = rememberSafePadding(
        handleStartInsets = false,
    )

    val paddingStart = safePadding.start + MaterialTheme.spacings.default
    val paddingEnd = safePadding.end + MaterialTheme.spacings.default
    val paddingBottom = safePadding.bottom + MaterialTheme.spacings.default

    val contentPaddingTop by animateDpAsState(
        targetValue = if (state.error != null) {
            safePadding.top + 144.dp
        } else {
            safePadding.top + 88.dp
        },
        label = "content_padding",
    )

    var showErrorDialog by rememberSaveable { mutableStateOf(false) }

    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val minColumnSize = when (windowSizeClass.windowWidthSizeClass) {
        WindowWidthSizeClass.EXPANDED -> 320.dp
        WindowWidthSizeClass.MEDIUM -> 240.dp
        else -> 160.dp
    }

    Box(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        FilmSearchBar(
            onSettingsClick = {
                onAction(MediaAction.OnSettingsClick)
            },
            modifier = Modifier.fillMaxWidth(),
            paddingStart = paddingStart,
            paddingEnd = paddingEnd,
            inputPaddingStart = safePadding.start,
            inputPaddingEnd = safePadding.end,
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = minColumnSize),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = paddingStart,
                top = contentPaddingTop,
                end = paddingEnd,
                bottom = paddingBottom,
            ),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.default),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.default),
        ) {
            item(
                span = { GridItemSpan(maxLineSpan) },
            ) {
                FavoritesCard(
                    onClick = {},
                )
            }
            items(state.libraries, key = { it.id }) { library ->
                ItemCard(
                    item = library,
                    direction = Direction.HORIZONTAL,
                    onClick = {
                        onAction(MediaAction.OnItemClick(library))
                    },
                    modifier = Modifier
                        .animateItem(),
                )
            }
        }
        if (state.error != null) {
            ErrorCard(
                onShowStacktrace = {
                    showErrorDialog = true
                },
                onRetryClick = {
                    onAction(MediaAction.OnRetryClick)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = paddingStart,
                        top = safePadding.top + 80.dp,
                        end = paddingEnd,
                    ),
            )
            if (showErrorDialog) {
                ErrorDialog(
                    exception = state.error!!,
                    onDismissRequest = { showErrorDialog = false },
                )
            }
        }
    }
}

@PreviewScreenSizes
@Composable
private fun MediaScreenLayoutPreview() {
    FindroidTheme {
        MediaScreenLayout(
            state = MediaState(
                libraries = dummyCollections,
                error = Exception("Failed to load data"),
            ),
            onAction = {},
        )
    }
}
