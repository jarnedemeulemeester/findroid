package dev.jdtech.jellyfin.presentation.film.components

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
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
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.utils.ObserveAsEvents
import dev.jdtech.jellyfin.utils.format
import dev.jdtech.jellyfin.viewmodels.PlayerItemsEvent
import dev.jdtech.jellyfin.viewmodels.PlayerViewModel
import java.util.UUID
import dev.jdtech.jellyfin.core.R as CoreR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeBottomSheet(
    episodeId: UUID,
    onDismissRequest: () -> Unit,
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
            is PlayerItemsEvent.PlayerItemsError -> Unit
        }
    }

    EpisodeBottomSheetLayout(
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
                else -> Unit
            }
            viewModel.onAction(action)
        },
        onDismissRequest = onDismissRequest,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EpisodeBottomSheetLayout(
    state: EpisodeState,
    isLoadingPlayer: Boolean,
    isLoadingRestartPlayer: Boolean,
    onAction: (EpisodeAction) -> Unit,
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(),
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = MaterialTheme.spacings.default,
                    end = MaterialTheme.spacings.default,
                    bottom = MaterialTheme.spacings.default,
                ),
        ) {
            state.episode?.let { episode ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Box(
                            modifier = Modifier.clip(MaterialTheme.shapes.small),
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(episode.images.primary)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                modifier = Modifier
                                    .width(142.dp)
                                    .aspectRatio(1.77f),
                                placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceContainer),
                                contentScale = ContentScale.Crop,
                            )
                            ProgressBar(
                                item = episode,
                                width = 142,
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(MaterialTheme.spacings.small),
                            )
                        }
                        Spacer(Modifier.width(MaterialTheme.spacings.default.div(2)))
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
                                style = MaterialTheme.typography.titleMedium,
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        PlayButton(
                            item = episode,
                            onClick = {
                                onAction(EpisodeAction.Play())
                            },
                            enabled = !isLoadingPlayer && !isLoadingRestartPlayer,
                            isLoading = isLoadingPlayer,
                        )
                        if (episode.playbackPositionTicks.div(600000000) > 0) {
                            FilledTonalIconButton(
                                onClick = {
                                    onAction(EpisodeAction.Play(startFromBeginning = true))
                                },
                                enabled = !isLoadingPlayer && !isLoadingRestartPlayer,
                            ) {
                                when (isLoadingRestartPlayer) {
                                    true -> {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = LocalContentColor.current,
                                        )
                                    }
                                    false -> {
                                        Icon(
                                            painter = painterResource(CoreR.drawable.ic_rotate_ccw),
                                            contentDescription = null,
                                        )
                                    }
                                }
                            }
                        }
                        FilledTonalIconButton(
                            onClick = {
                                when (episode.played) {
                                    true -> onAction(EpisodeAction.UnmarkAsPlayed)
                                    false -> onAction(EpisodeAction.MarkAsPlayed)
                                }
                            },
                        ) {
                            Icon(
                                painter = painterResource(CoreR.drawable.ic_check),
                                contentDescription = null,
                                tint = if (episode.played) Color.Red else LocalContentColor.current,
                            )
                        }
                        FilledTonalIconButton(
                            onClick = {
                                when (episode.favorite) {
                                    true -> onAction(EpisodeAction.UnmarkAsFavorite)
                                    false -> onAction(EpisodeAction.MarkAsFavorite)
                                }
                            },
                        ) {
                            when (episode.favorite) {
                                true -> {
                                    Icon(
                                        painter = painterResource(CoreR.drawable.ic_heart_filled),
                                        contentDescription = null,
                                        tint = Color.Red,
                                    )
                                }
                                false -> {
                                    Icon(
                                        painter = painterResource(CoreR.drawable.ic_heart),
                                        contentDescription = null,
                                    )
                                }
                            }
                        }
                        if (episode.canDownload) {
                            FilledTonalIconButton(
                                onClick = {},
                            ) {
                                Icon(
                                    painter = painterResource(CoreR.drawable.ic_download),
                                    contentDescription = null,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(MaterialTheme.spacings.small))
                    Text(
                        text = episode.overview,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun EpisodeBottomSheetLayoutPreview() {
    FindroidTheme {
        EpisodeBottomSheetLayout(
            state = EpisodeState(
                episode = dummyEpisode,
            ),
            isLoadingPlayer = false,
            isLoadingRestartPlayer = false,
            onAction = {},
            onDismissRequest = {},
            sheetState = rememberStandardBottomSheetState(initialValue = SheetValue.Expanded),
        )
    }
}
