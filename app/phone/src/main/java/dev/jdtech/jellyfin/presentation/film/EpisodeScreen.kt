package dev.jdtech.jellyfin.presentation.film

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import android.widget.Toast
import androidx.core.graphics.toColorInt
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jdtech.jellyfin.PlayerActivity
import dev.jdtech.jellyfin.core.presentation.dummy.dummyEpisode
import dev.jdtech.jellyfin.core.presentation.dummy.dummyVideoMetadata
import dev.jdtech.jellyfin.film.presentation.episode.EpisodeAction
import dev.jdtech.jellyfin.film.presentation.episode.EpisodeState
import dev.jdtech.jellyfin.film.presentation.episode.EpisodeViewModel
import dev.jdtech.jellyfin.presentation.film.components.ActorsRow
import dev.jdtech.jellyfin.presentation.film.components.ItemButtonsBar
import dev.jdtech.jellyfin.presentation.film.components.ItemHeader
import dev.jdtech.jellyfin.presentation.film.components.OverviewText
import dev.jdtech.jellyfin.presentation.film.components.VideoMetadataBar
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.presentation.utils.rememberSafePadding
import dev.jdtech.jellyfin.dialogs.getStorageSelectionDialog
import dev.jdtech.jellyfin.presentation.downloads.DownloaderEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import dev.jdtech.jellyfin.utils.format
import org.jellyfin.sdk.model.api.BaseItemKind
import java.util.UUID
import dev.jdtech.jellyfin.core.R as CoreR

@Composable
fun EpisodeScreen(
    episodeId: UUID,
    navigateBack: () -> Unit,
    navigateToPerson: (personId: UUID) -> Unit,
    viewModel: EpisodeViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(true) {
        viewModel.loadEpisode(episodeId = episodeId)
    }

    EpisodeScreenLayout(
        state = state,
        onAction = { action ->
            when (action) {
                is EpisodeAction.Play -> {
                    val intent = Intent(context, PlayerActivity::class.java)
                    intent.putExtra("itemId", episodeId.toString())
                    intent.putExtra("itemKind", BaseItemKind.EPISODE.serialName)
                    intent.putExtra("startFromBeginning", action.startFromBeginning)
                    context.startActivity(intent)
                }
                is EpisodeAction.Download -> {
                    state.episode?.let { episode ->
                        val dialog = getStorageSelectionDialog(
                            context = context,
                            onItemSelected = { which ->
                                val appContext = context.applicationContext
                                val downloader = EntryPointAccessors.fromApplication(appContext, DownloaderEntryPoint::class.java).downloader()
                                CoroutineScope(Dispatchers.IO).launch {
                                    val sourceId = episode.sources.firstOrNull()?.id ?: return@launch
                                    val result = downloader.downloadItem(episode, sourceId, which)
                                    if (result.first > 0) {
                                        launch(Dispatchers.Main) {
                                            Toast.makeText(context, context.getString(CoreR.string.download_started), Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        launch(Dispatchers.Main) {
                                            Toast.makeText(context, result.second?.asString(context.resources) ?: context.getString(CoreR.string.unknown_error), Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            },
                            onCancel = { /* no-op */ },
                        )
                        dialog.show()
                    }
                }
                is EpisodeAction.OnBackClick -> navigateBack()
                is EpisodeAction.NavigateToPerson -> navigateToPerson(action.personId)
                else -> Unit
            }
            viewModel.onAction(action)
        },
    )
}

@Composable
private fun EpisodeScreenLayout(
    state: EpisodeState,
    onAction: (EpisodeAction) -> Unit,
) {
    val appContext = LocalContext.current.applicationContext
    val safePadding = rememberSafePadding()

    val paddingStart = safePadding.start + MaterialTheme.spacings.default
    val paddingEnd = safePadding.end + MaterialTheme.spacings.default
    val paddingBottom = safePadding.bottom + MaterialTheme.spacings.default

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        state.episode?.let { episode ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
            ) {
                ItemHeader(
                    item = episode,
                    scrollState = scrollState,
                    content = {
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(
                                    start = paddingStart,
                                    end = paddingEnd,
                                ),
                        ) {
                            Text(
                                text = stringResource(
                                    id = CoreR.string.season_episode,
                                    episode.parentIndexNumber,
                                    episode.indexNumber,
                                ),
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                                style = MaterialTheme.typography.labelLarge,
                            )
                            Text(
                                text = episode.name,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 3,
                                style = MaterialTheme.typography.headlineMedium,
                            )
                        }
                    },
                )
                Column(
                    modifier = Modifier.padding(
                        start = paddingStart,
                        end = paddingEnd,
                    ),
                ) {
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
                        onDownloadClick = { onAction(EpisodeAction.Download) },
                        onDeleteClick = {
                            val local = episode.sources.firstOrNull { it.type == dev.jdtech.jellyfin.models.FindroidSourceType.LOCAL }
                            if (local != null) {
                                val downloader = EntryPointAccessors.fromApplication(appContext, DownloaderEntryPoint::class.java).downloader()
                                CoroutineScope(Dispatchers.IO).launch {
                                    downloader.deleteItem(episode, local)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(MaterialTheme.spacings.small))
                    OverviewText(
                        text = episode.overview,
                    )
                    Spacer(Modifier.height(MaterialTheme.spacings.medium))
                }
                if (state.actors.isNotEmpty()) {
                    ActorsRow(
                        actors = state.actors,
                        onActorClick = { personId ->
                            onAction(EpisodeAction.NavigateToPerson(personId))
                        },
                        contentPadding = PaddingValues(
                            start = paddingStart,
                            end = paddingEnd,
                        ),
                    )
                }
                Spacer(Modifier.height(paddingBottom))
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
                .padding(
                    start = safePadding.start + MaterialTheme.spacings.small,
                    top = safePadding.top + MaterialTheme.spacings.small,
                    end = safePadding.end + MaterialTheme.spacings.small,
                ),
        ) {
            IconButton(
                onClick = { onAction(EpisodeAction.OnBackClick) },
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
private fun EpisodeScreenLayoutPreview() {
    FindroidTheme {
        EpisodeScreenLayout(
            state = EpisodeState(
                episode = dummyEpisode,
                videoMetadata = dummyVideoMetadata,
            ),
            onAction = {},
        )
    }
}
