package dev.jdtech.jellyfin.presentation.film

import android.content.Context
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jdtech.jellyfin.PlayerActivity
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.core.presentation.downloader.DownloaderAction
import dev.jdtech.jellyfin.core.presentation.downloader.DownloaderEvent
import dev.jdtech.jellyfin.core.presentation.downloader.DownloaderState
import dev.jdtech.jellyfin.core.presentation.downloader.DownloaderViewModel
import dev.jdtech.jellyfin.core.presentation.dummy.dummyEpisode
import dev.jdtech.jellyfin.core.presentation.dummy.dummyVideoMetadata
import dev.jdtech.jellyfin.film.presentation.episode.EpisodeAction
import dev.jdtech.jellyfin.film.presentation.episode.EpisodeState
import dev.jdtech.jellyfin.film.presentation.episode.EpisodeViewModel
import dev.jdtech.jellyfin.presentation.film.components.ActorsRow
import dev.jdtech.jellyfin.presentation.film.components.ExtraInfoText
import dev.jdtech.jellyfin.presentation.film.components.ItemButtonsBar
import dev.jdtech.jellyfin.presentation.film.components.ItemHeader
import dev.jdtech.jellyfin.presentation.film.components.ItemTopBar
import dev.jdtech.jellyfin.presentation.film.components.OverviewText
import dev.jdtech.jellyfin.presentation.film.components.VersionSelectionDialog
import dev.jdtech.jellyfin.presentation.film.components.VideoMetadataBar
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.presentation.utils.LocalOfflineMode
import dev.jdtech.jellyfin.presentation.utils.rememberSafePadding
import dev.jdtech.jellyfin.utils.ObserveAsEvents
import dev.jdtech.jellyfin.utils.format
import java.util.UUID
import org.jellyfin.sdk.model.api.BaseItemKind

@Composable
fun EpisodeScreen(
    episodeId: UUID,
    navigateBack: () -> Unit,
    navigateHome: () -> Unit,
    navigateToPerson: (personId: UUID) -> Unit,
    navigateToSeason: (seasonId: UUID) -> Unit,
    viewModel: EpisodeViewModel = hiltViewModel(),
    downloaderViewModel: DownloaderViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val isOfflineMode = LocalOfflineMode.current

    val state by viewModel.state.collectAsStateWithLifecycle()
    val downloaderState by downloaderViewModel.state.collectAsStateWithLifecycle()
    var hasMultipleMediaSources by remember { mutableStateOf(false) }
    var showMediaSourceSelectorDialog by remember { mutableStateOf(false) }
    var playMedia by remember { mutableStateOf(false) }
    var playDataItemId by remember { mutableStateOf("") }
    var playDataItemKind by remember { mutableStateOf("") }
    var playDataMediaSourceId by remember { mutableStateOf("") }
    var playDataStartFromBeginning by remember { mutableStateOf(false) }
    val mediaSources = remember { mutableListOf<Pair<String, String>>() }

    LaunchedEffect(true) { viewModel.loadEpisode(episodeId = episodeId) }

    LaunchedEffect(state.episode) {
        state.episode?.let { episode -> downloaderViewModel.update(episode) }
    }

    ObserveAsEvents(downloaderViewModel.events) { event ->
        when (event) {
            is DownloaderEvent.Successful -> {
                viewModel.loadEpisode(episodeId = episodeId)
            }
            is DownloaderEvent.Deleted -> {
                if (isOfflineMode) {
                    navigateBack()
                } else {
                    viewModel.loadEpisode(episodeId = episodeId)
                }
            }
        }
    }

    if ((state.episode?.sources?.size ?: 1) > 1) {
        hasMultipleMediaSources = true
        if (state.episode?.sources != null) {
            for (source in state.episode?.sources!!) {
                mediaSources.remove(Pair(source.id, source.name))
                mediaSources.add(Pair(source.id, source.name))
            }
        }
    } else {
        hasMultipleMediaSources = false
    }

    EpisodeScreenLayout(
        state = state,
        downloaderState = downloaderState,
        onAction = { action ->
            when (action) {
                is EpisodeAction.Play -> {
                    playDataItemId = episodeId.toString()
                    playDataItemKind = BaseItemKind.EPISODE.serialName
                    playDataStartFromBeginning = action.startFromBeginning
                    if (hasMultipleMediaSources) {
                        showMediaSourceSelectorDialog = true
                        playMedia = false
                    } else {
                        playMedia = true
                    }
                }
                is EpisodeAction.OnBackClick -> navigateBack()
                is EpisodeAction.OnHomeClick -> navigateHome()
                is EpisodeAction.NavigateToPerson -> navigateToPerson(action.personId)
                is EpisodeAction.NavigateToSeason -> navigateToSeason(action.seasonId)
                else -> Unit
            }
            viewModel.onAction(action)
        },
        onDownloaderAction = { action -> downloaderViewModel.onAction(action) },
    )

    if (showMediaSourceSelectorDialog) {
        playMedia = false
        VersionSelectionDialog(
            mediaSources = mediaSources,
            onSelect = { mediaSourceId ->
                playDataMediaSourceId = mediaSourceId
                showMediaSourceSelectorDialog = false
                playMedia = true
            },
            onDismiss = { showMediaSourceSelectorDialog = false },
        )
    }

    if (playMedia) {
        playMedia = false
        PlayMedia(
            context = context,
            itemId = playDataItemId,
            itemKind = playDataItemKind,
            mediaSourceId = playDataMediaSourceId,
            startFromBeginning = playDataStartFromBeginning,
        )
    }
}

@Composable
private fun PlayMedia(
    context: Context,
    itemId: String,
    itemKind: String,
    mediaSourceId: String? = null,
    startFromBeginning: Boolean,
) {
    val intent = Intent(context, PlayerActivity::class.java)
    intent.putExtra("itemId", itemId)
    intent.putExtra("itemKind", itemKind)
    intent.putExtra("mediaSourceId", mediaSourceId)
    intent.putExtra("startFromBeginning", startFromBeginning)
    context.startActivity(intent)
}

@Composable
private fun EpisodeScreenLayout(
    state: EpisodeState,
    downloaderState: DownloaderState,
    onAction: (EpisodeAction) -> Unit,
    onDownloaderAction: (DownloaderAction) -> Unit,
) {
    val safePadding = rememberSafePadding()

    val paddingStart = safePadding.start + MaterialTheme.spacings.default
    val paddingEnd = safePadding.end + MaterialTheme.spacings.default
    val paddingBottom = safePadding.bottom + MaterialTheme.spacings.default

    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        state.episode?.let { episode ->
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(scrollState)) {
                ItemHeader(
                    item = episode,
                    scrollState = scrollState,
                    content = {
                        Column(
                            modifier =
                                Modifier.align(Alignment.BottomStart)
                                    .padding(start = paddingStart, end = paddingEnd)
                        ) {
                            val seasonName =
                                episode.seasonName
                                    ?: run {
                                        stringResource(
                                            CoreR.string.season_number,
                                            episode.parentIndexNumber,
                                        )
                                    }
                            Text(
                                text =
                                    "$seasonName - " +
                                        stringResource(
                                            id = CoreR.string.episode_number,
                                            episode.indexNumber,
                                        ),
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
                Column(modifier = Modifier.padding(start = paddingStart, end = paddingEnd)) {
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
                            text =
                                stringResource(
                                    CoreR.string.runtime_minutes,
                                    episode.runtimeTicks.div(600000000),
                                ),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        episode.communityRating?.let { communityRating ->
                            Row(verticalAlignment = Alignment.Bottom) {
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
                        downloaderState = downloaderState,
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
                        onDownloadClick = { storageIndex ->
                            onDownloaderAction(DownloaderAction.Download(episode, storageIndex))
                        },
                        onDownloadCancelClick = {
                            onDownloaderAction(DownloaderAction.CancelDownload(episode))
                        },
                        onDownloadDeleteClick = {
                            onDownloaderAction(DownloaderAction.DeleteDownload(episode))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(MaterialTheme.spacings.small))
                    if (state.displayExtraInfo && state.videoMetadata != null) {
                        ExtraInfoText(videoMetadata = state.videoMetadata!!)
                        Spacer(Modifier.height(MaterialTheme.spacings.medium))
                    }
                    OverviewText(text = episode.overview)
                    Spacer(Modifier.height(MaterialTheme.spacings.medium))
                }
                if (state.actors.isNotEmpty()) {
                    ActorsRow(
                        actors = state.actors,
                        onActorClick = { personId ->
                            onAction(EpisodeAction.NavigateToPerson(personId))
                        },
                        contentPadding = PaddingValues(start = paddingStart, end = paddingEnd),
                    )
                }
                Spacer(Modifier.height(paddingBottom))
            }
        } ?: run { CircularProgressIndicator(modifier = Modifier.align(Alignment.Center)) }

        ItemTopBar(
            hasBackButton = true,
            hasHomeButton = true,
            onBackClick = { onAction(EpisodeAction.OnBackClick) },
            onHomeClick = { onAction(EpisodeAction.OnHomeClick) },
        ) {
            Spacer(modifier = Modifier.width(4.dp))
            state.episode?.let { episode ->
                Button(
                    onClick = { onAction(EpisodeAction.NavigateToSeason(episode.seasonId)) },
                    modifier = Modifier.alpha(0.7f),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = Color.Black,
                            contentColor = Color.White,
                        ),
                ) {
                    episode.seasonName?.let { seasonName -> Text(seasonName) }
                        ?: run {
                            Text(
                                stringResource(
                                    CoreR.string.season_number,
                                    episode.parentIndexNumber,
                                )
                            )
                        }
                }
            }
        }
    }
}

@PreviewScreenSizes
@Composable
private fun EpisodeScreenLayoutPreview() {
    FindroidTheme {
        EpisodeScreenLayout(
            state = EpisodeState(episode = dummyEpisode, videoMetadata = dummyVideoMetadata),
            downloaderState = DownloaderState(),
            onAction = {},
            onDownloaderAction = {},
        )
    }
}
