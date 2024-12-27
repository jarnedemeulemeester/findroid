package dev.jdtech.jellyfin.presentation.film

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jdtech.jellyfin.film.presentation.media.MediaAction
import dev.jdtech.jellyfin.film.presentation.media.MediaState
import dev.jdtech.jellyfin.film.presentation.media.MediaViewModel
import dev.jdtech.jellyfin.presentation.film.components.Direction
import dev.jdtech.jellyfin.presentation.film.components.FilmSearchBar
import dev.jdtech.jellyfin.presentation.film.components.ItemCard
import dev.jdtech.jellyfin.presentation.theme.spacings

@Composable
fun MediaScreen(
    viewModel: MediaViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(true) {
        viewModel.loadData()
    }

    MediaScreenLayout(
        state = state,
        onAction = { action ->
            viewModel.onAction(action)
        },
    )
}

@Composable
private fun MediaScreenLayout(
    state: MediaState,
    onAction: (MediaAction) -> Unit,
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    val safePaddingStart = with(density) { WindowInsets.safeDrawing.getLeft(this, layoutDirection).toDp() }
    val safePaddingEnd = with(density) { WindowInsets.safeDrawing.getRight(this, layoutDirection).toDp() }

    val paddingStart = safePaddingStart + MaterialTheme.spacings.default
    val paddingEnd = safePaddingEnd + MaterialTheme.spacings.default

    val contentPaddingTop by animateDpAsState(
        targetValue = if (state.error != null) {
            with(density) { WindowInsets.safeDrawing.getTop(this).toDp() + 136.dp }
        } else {
            with(density) { WindowInsets.safeDrawing.getTop(this).toDp() + 88.dp }
        },
        label = "content_padding",
    )

    Box {
        FilmSearchBar(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { traversalIndex = 0f },
            paddingStart = paddingStart,
            paddingEnd = paddingEnd,
            inputPaddingStart = safePaddingStart,
            inputPaddingEnd = safePaddingEnd,
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            contentPadding = PaddingValues(
                start = paddingStart,
                top = contentPaddingTop,
                end = paddingEnd,
                bottom = with(density) { WindowInsets.safeDrawing.getBottom(this).toDp() + MaterialTheme.spacings.default },
            ),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.default),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.default),
        ) {
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
    }
}
