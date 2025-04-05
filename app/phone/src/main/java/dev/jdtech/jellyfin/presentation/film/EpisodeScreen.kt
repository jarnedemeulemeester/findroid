package dev.jdtech.jellyfin.presentation.film

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import dev.jdtech.jellyfin.PlayerActivity
import dev.jdtech.jellyfin.core.presentation.dummy.dummyEpisode
import dev.jdtech.jellyfin.film.presentation.episode.EpisodeAction
import dev.jdtech.jellyfin.film.presentation.episode.EpisodeState
import dev.jdtech.jellyfin.film.presentation.episode.EpisodeViewModel
import dev.jdtech.jellyfin.presentation.film.components.ItemButtonsBar
import dev.jdtech.jellyfin.presentation.film.components.PersonItem
import dev.jdtech.jellyfin.presentation.film.components.VideoMetadataBar
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.presentation.utils.parallaxLayoutModifier
import dev.jdtech.jellyfin.utils.ObserveAsEvents
import dev.jdtech.jellyfin.utils.format
import dev.jdtech.jellyfin.viewmodels.PlayerItemsEvent
import dev.jdtech.jellyfin.viewmodels.PlayerViewModel
import java.util.UUID
import dev.jdtech.jellyfin.core.R as CoreR

@Composable
fun EpisodeScreen(
    episodeId: UUID,
    navigateBack: () -> Unit,
    viewModel: EpisodeViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    var isLoadingPlayer by remember { mutableStateOf(false) }
    var isLoadingRestartPlayer by remember { mutableStateOf(false) }

    LaunchedEffect(true) {
        viewModel.loadEpisode(episodeId = episodeId)
    }

    ObserveAsEvents(playerViewModel.eventsChannelFlow) { event ->
        when (event) {
            is PlayerItemsEvent.PlayerItemsReady -> {
                isLoadingPlayer = false
                isLoadingRestartPlayer = false
                val intent = Intent(context, PlayerActivity::class.java)
                intent.putExtra("items", ArrayList(event.items))
                context.startActivity(intent)
            }
            is PlayerItemsEvent.PlayerItemsError -> {
                isLoadingPlayer = false
                isLoadingRestartPlayer = false
                Toast.makeText(context, CoreR.string.error_preparing_player_items, Toast.LENGTH_LONG).show()
            }
        }
    }

    EpisodeScreenLayout(
        state = state,
        isLoadingPlayer = isLoadingPlayer,
        isLoadingRestartPlayer = isLoadingRestartPlayer,
        onAction = { action ->
            when (action) {
                is EpisodeAction.Play -> {
                    when (action.startFromBeginning) {
                        true -> isLoadingRestartPlayer = true
                        false -> isLoadingPlayer = true
                    }
                    state.episode?.let { episode ->
                        playerViewModel.loadPlayerItems(episode, startFromBeginning = action.startFromBeginning)
                    }
                }
                is EpisodeAction.OnBackClick -> navigateBack()
                else -> Unit
            }
            viewModel.onAction(action)
        },
    )
}

@Composable
private fun EpisodeScreenLayout(
    state: EpisodeState,
    isLoadingPlayer: Boolean,
    isLoadingRestartPlayer: Boolean,
    onAction: (EpisodeAction) -> Unit,
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    val safePaddingStart = with(density) { WindowInsets.safeDrawing.getLeft(this, layoutDirection).toDp() }
    val safePaddingEnd = with(density) { WindowInsets.safeDrawing.getRight(this, layoutDirection).toDp() }
    val safePaddingBottom = with(density) { WindowInsets.safeDrawing.getBottom(this).toDp() }

    val paddingStart = safePaddingStart + MaterialTheme.spacings.default
    val paddingEnd = safePaddingEnd + MaterialTheme.spacings.default
    val paddingBottom = safePaddingBottom + MaterialTheme.spacings.default

    val backgroundColor = MaterialTheme.colorScheme.background

    val scrollState = rememberScrollState()

    state.episode?.let { episode ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState),
        ) {
            Box(
                modifier = Modifier
                    .height(240.dp)
                    .clipToBounds(),
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(episode.images.primary)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .parallaxLayoutModifier(
                            scrollState = scrollState,
                            2,
                        ),
                    placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceContainer),
                    contentScale = ContentScale.Crop,
                )
                Canvas(
                    modifier = Modifier
                        .fillMaxSize(),
                ) {
                    drawRect(
                        Color.Black.copy(alpha = 0.2f),
                    )
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, backgroundColor),
                            startY = size.height / 2,
                        ),
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .safeDrawingPadding()
                        .padding(start = 8.dp, end = 8.dp),
                ) {
                    IconButton(
                        onClick = { onAction(EpisodeAction.OnBackClick) },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color.Black.copy(alpha = 0.4f),
                            contentColor = Color.White.copy(alpha = 0.8f),
                        ),
                    ) {
                        Icon(
                            painter = painterResource(CoreR.drawable.ic_arrow_left),
                            contentDescription = null,
                        )
                    }
                }
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(episode.images.showLogo)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(MaterialTheme.spacings.default)
                        .height(100.dp)
                        .fillMaxWidth(),
                    contentScale = ContentScale.Fit,
                )
            }
            Column(
                modifier = Modifier.padding(
                    start = paddingStart,
                    end = paddingEnd,
                ),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column {
                        Text(
                            text = episode.seriesName,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = stringResource(
                                id = CoreR.string.episode_name_extended,
                                episode.parentIndexNumber,
                                episode.indexNumber,
                                episode.name,
                            ),
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 3,
                            style = MaterialTheme.typography.headlineMedium,
                        )
                    }
                }
                Spacer(Modifier.height(MaterialTheme.spacings.small))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    episode.premiereDate?.let { premiereDate ->
                        Text(
                            text = premiereDate.format(),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Text(
                        text = stringResource(CoreR.string.runtime_minutes, episode.runtimeTicks.div(600000000)),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    episode.communityRating?.let { communityRating ->
                        Row(
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            Icon(
                                painter = painterResource(CoreR.drawable.ic_star),
                                contentDescription = null,
                                tint = Color("#F2C94C".toColorInt()),
                            )
                            Spacer(Modifier.width(MaterialTheme.spacings.extraSmall))
                            Text(
                                text = "%.1f".format(communityRating),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(MaterialTheme.spacings.small))
                state.videoMetadata?.let { videoMetadata ->
                    VideoMetadataBar(videoMetadata)
                    Spacer(Modifier.height(MaterialTheme.spacings.small))
                }
                ItemButtonsBar(
                    item = episode,
                    onPlayClick = { startFromBeginning ->
                        onAction(EpisodeAction.Play(startFromBeginning = startFromBeginning))
                    },
                    onMarkAsPlayedClick = {
                        when (episode.played) {
                            true -> onAction(EpisodeAction.UnmarkAsPlayed)
                            false -> onAction(EpisodeAction.MarkAsPlayed)
                        }
                    },
                    onMarkAsFavoriteClick = {
                        when (episode.favorite) {
                            true -> onAction(EpisodeAction.UnmarkAsFavorite)
                            false -> onAction(EpisodeAction.MarkAsFavorite)
                        }
                    },
                    onTrailerClick = {},
                    onDownloadClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    isLoadingPlayer = isLoadingPlayer,
                    isLoadingRestartPlayer = isLoadingRestartPlayer,
                )
                Spacer(Modifier.height(MaterialTheme.spacings.small))
                Text(
                    text = episode.overview,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(MaterialTheme.spacings.medium))
            }
            if (episode.people.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .padding(
                            start = paddingStart,
                            end = paddingEnd,
                        ),
                ) {
                    Text(
                        text = stringResource(CoreR.string.cast_amp_crew),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(MaterialTheme.spacings.small))
                }
                LazyRow(
                    contentPadding = PaddingValues(
                        start = paddingStart,
                        end = paddingEnd,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
                ) {
                    items(
                        items = episode.people,
                        key = { person ->
                            person.id
                        },
                    ) { person ->
                        PersonItem(
                            person = person,
                        )
                    }
                }
            }
            Spacer(Modifier.height(paddingBottom))
        }
    }
}

@PreviewScreenSizes
@Composable
private fun EpisodeScreenLayoutPreview() {
    FindroidTheme {
        EpisodeScreenLayout(
            state = EpisodeState(
                episode = dummyEpisode,
            ),
            isLoadingPlayer = false,
            isLoadingRestartPlayer = false,
            onAction = {},
        )
    }
}
