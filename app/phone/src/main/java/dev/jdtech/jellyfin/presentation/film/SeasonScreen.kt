package dev.jdtech.jellyfin.presentation.film

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jdtech.jellyfin.core.presentation.dummy.dummySeason
import dev.jdtech.jellyfin.film.presentation.season.SeasonAction
import dev.jdtech.jellyfin.film.presentation.season.SeasonState
import dev.jdtech.jellyfin.film.presentation.season.SeasonViewModel
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.presentation.film.components.Direction
import dev.jdtech.jellyfin.presentation.film.components.EpisodeCard
import dev.jdtech.jellyfin.presentation.film.components.ItemButtonsBar
import dev.jdtech.jellyfin.presentation.film.components.ItemHeader
import dev.jdtech.jellyfin.presentation.film.components.ItemPoster
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.viewmodels.PlayerViewModel
import java.util.UUID
import dev.jdtech.jellyfin.core.R as CoreR

@Composable
fun SeasonScreen(
    seasonId: UUID,
    navigateBack: () -> Unit,
    navigateToItem: (item: FindroidItem) -> Unit,
    viewModel: SeasonViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var isLoadingPlayer by remember { mutableStateOf(false) }
    var isLoadingRestartPlayer by remember { mutableStateOf(false) }

    LaunchedEffect(true) {
        viewModel.loadSeason(seasonId = seasonId)
    }

    SeasonScreenLayout(
        state = state,
        isLoadingPlayer = isLoadingPlayer,
        isLoadingRestartPlayer = isLoadingRestartPlayer,
        onAction = { action ->
            when (action) {
                is SeasonAction.Play -> {
                    when (action.startFromBeginning) {
                        true -> isLoadingRestartPlayer = true
                        false -> isLoadingPlayer = true
                    }
                    state.season?.let { show ->
                        playerViewModel.loadPlayerItems(show, startFromBeginning = action.startFromBeginning)
                    }
                }
                is SeasonAction.OnBackClick -> navigateBack()
                is SeasonAction.NavigateToItem -> navigateToItem(action.item)
                else -> Unit
            }
            viewModel.onAction(action)
        },
    )
}

@Composable
private fun SeasonScreenLayout(
    state: SeasonState,
    isLoadingPlayer: Boolean,
    isLoadingRestartPlayer: Boolean,
    onAction: (SeasonAction) -> Unit,
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    val safePaddingStart = with(density) { WindowInsets.safeDrawing.getLeft(this, layoutDirection).toDp() }
    val safePaddingEnd = with(density) { WindowInsets.safeDrawing.getRight(this, layoutDirection).toDp() }
    val safePaddingBottom = with(density) { WindowInsets.safeDrawing.getBottom(this).toDp() }

    val paddingStart = safePaddingStart + MaterialTheme.spacings.default
    val paddingEnd = safePaddingEnd + MaterialTheme.spacings.default
    val paddingBottom = safePaddingBottom + MaterialTheme.spacings.default

    val lazyListState = rememberLazyListState()

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        state.season?.let { season ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth(),
                state = lazyListState,
                contentPadding = PaddingValues(bottom = paddingBottom),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.default),
            ) {
                item {
                    ItemHeader(
                        item = season,
                        lazyListState = lazyListState,
                        content = {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(
                                        start = paddingStart,
                                        end = paddingEnd,
                                    ),
                                verticalAlignment = Alignment.Bottom,
                            ) {
                                ItemPoster(
                                    item = season,
                                    direction = Direction.VERTICAL,
                                    modifier = Modifier
                                        .width(120.dp)
                                        .clip(MaterialTheme.shapes.small),
                                )
                                Spacer(Modifier.width(MaterialTheme.spacings.medium))
                                Column(
                                    modifier = Modifier,
                                ) {
                                    Text(
                                        text = season.seriesName,
                                        overflow = TextOverflow.Ellipsis,
                                        maxLines = 1,
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                    Text(
                                        text = season.name,
                                        overflow = TextOverflow.Ellipsis,
                                        maxLines = 3,
                                        style = MaterialTheme.typography.headlineMedium,
                                    )
                                }
                            }
                        },
                    )
                    Spacer(Modifier.height(MaterialTheme.spacings.medium))
                    ItemButtonsBar(
                        item = season,
                        onPlayClick = { startFromBeginning ->
                            onAction(SeasonAction.Play(startFromBeginning = startFromBeginning))
                        },
                        onMarkAsPlayedClick = {
                            when (season.played) {
                                true -> onAction(SeasonAction.UnmarkAsPlayed)
                                false -> onAction(SeasonAction.MarkAsPlayed)
                            }
                        },
                        onMarkAsFavoriteClick = {
                            when (season.favorite) {
                                true -> onAction(SeasonAction.UnmarkAsFavorite)
                                false -> onAction(SeasonAction.MarkAsFavorite)
                            }
                        },
                        onTrailerClick = {},
                        onDownloadClick = {},
                        modifier = Modifier
                            .padding(
                                start = paddingStart,
                                end = paddingEnd,
                            )
                            .fillMaxWidth(),
                        isLoadingPlayer = isLoadingPlayer,
                        isLoadingRestartPlayer = isLoadingRestartPlayer,
                    )
                }
                items(
                    items = state.episodes,
                    key = { episode -> episode.id },
                ) { episode ->
                    EpisodeCard(
                        episode = episode,
                        onClick = {
                            onAction(SeasonAction.NavigateToItem(episode))
                        },
                        modifier = Modifier
                            .padding(
                                start = paddingStart,
                                end = paddingEnd,
                            ),
                    )
                }
            }
        } ?: run {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .safeDrawingPadding()
                .padding(horizontal = MaterialTheme.spacings.small),
        ) {
            IconButton(
                onClick = { onAction(SeasonAction.OnBackClick) },
                modifier = Modifier
                    .alpha(0.7f),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Black,
                    contentColor = Color.White,
                ),
            ) {
                Icon(
                    painter = painterResource(CoreR.drawable.ic_arrow_left),
                    contentDescription = null,
                )
            }
        }
    }
}

@PreviewScreenSizes
@Composable
private fun SeasonScreenLayoutPreview() {
    FindroidTheme {
        SeasonScreenLayout(
            state = SeasonState(
                season = dummySeason,
            ),
            isLoadingPlayer = false,
            isLoadingRestartPlayer = false,
            onAction = {},
        )
    }
}
