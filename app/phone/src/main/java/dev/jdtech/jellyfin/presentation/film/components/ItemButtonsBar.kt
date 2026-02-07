package dev.jdtech.jellyfin.presentation.film.components

import android.app.DownloadManager
import android.os.Environment
import android.os.StatFs
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.core.presentation.downloader.DownloaderState
import dev.jdtech.jellyfin.core.presentation.dummy.dummyEpisode
import dev.jdtech.jellyfin.film.presentation.media.MediaActionKind
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.FindroidMovie
import dev.jdtech.jellyfin.models.FindroidShow
import dev.jdtech.jellyfin.models.isDownloaded
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings

@Composable
fun ItemButtonsBar(
    item: FindroidItem,
    onPlayClick: (playOptions: Pair<Boolean, String>) -> Unit,
    onMarkAsPlayedClick: () -> Unit,
    onMarkAsFavoriteClick: () -> Unit,
    onDownloadClick: (downloadOptions: Pair<Int, String>) -> Unit,
    onDownloadCancelClick: () -> Unit,
    onDownloadDeleteClick: () -> Unit,
    onTrailerClick: (uri: String) -> Unit,
    modifier: Modifier = Modifier,
    downloaderState: DownloaderState? = null,
    canPlay: Boolean = true,
) {
    val context = LocalContext.current
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass

    val trailerUri =
        when (item) {
            is FindroidMovie -> {
                item.trailer
            }
            is FindroidShow -> {
                item.trailer
            }
            else -> null
        }

    var storageSelectionDialogOpen by remember { mutableStateOf(false) }
    var cancelDownloadDialogOpen by remember { mutableStateOf(false) }
    var deleteDownloadDialogOpen by remember { mutableStateOf(false) }

    var selectedStorageIndex by remember { mutableIntStateOf(0) }
    var storageLocations = remember { context.getExternalFilesDirs(null) }

    var showMediaSourceSelectorDialog by remember { mutableStateOf(false) }
    var hasMultipleMediaSources by remember { mutableStateOf(false) }
    var selectedMediaSourceId by remember { mutableStateOf("") }
    val mediaSources = remember { mutableListOf<Pair<String, String>>() }

    var mediaActionKind by remember { mutableStateOf(MediaActionKind.NONE) }
    var mediaStartFromBeginning by remember { mutableStateOf(false) }
    var processMedaiAction by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
        ) {
            if (
                !windowSizeClass.isWidthAtLeastBreakpoint(
                    WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND
                )
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small)) {
                    PlayButton(
                        item = item,
                        onClick = {
                            mediaActionKind = MediaActionKind.PLAY
                            mediaStartFromBeginning = false
                            showMediaSourceSelectorDialog = true
                        },
                        modifier = Modifier.weight(weight = 1f, fill = true),
                        enabled = item.canPlay && canPlay,
                    )
                    if (item.playbackPositionTicks.div(600000000) > 0) {
                        FilledTonalIconButton(
                            onClick = {
                                mediaActionKind = MediaActionKind.PLAY
                                mediaStartFromBeginning = true
                                showMediaSourceSelectorDialog = true
                            }
                        ) {
                            Icon(
                                painter = painterResource(CoreR.drawable.ic_rotate_ccw),
                                contentDescription = null,
                            )
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small)) {
                if (
                    windowSizeClass.isWidthAtLeastBreakpoint(
                        WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND
                    )
                ) {
                    PlayButton(
                        item = item,
                        onClick = {
                            mediaActionKind = MediaActionKind.PLAY
                            mediaStartFromBeginning = false
                            showMediaSourceSelectorDialog = true
                        },
                        enabled = item.canPlay && canPlay,
                    )
                    if (item.playbackPositionTicks.div(600000000) > 0) {
                        FilledTonalIconButton(
                            onClick = {
                                mediaActionKind = MediaActionKind.PLAY
                                mediaStartFromBeginning = true
                                showMediaSourceSelectorDialog = true
                            }
                        ) {
                            Icon(
                                painter = painterResource(CoreR.drawable.ic_rotate_ccw),
                                contentDescription = null,
                            )
                        }
                    }
                }
                trailerUri?.let { uri ->
                    FilledTonalIconButton(onClick = { onTrailerClick(uri) }) {
                        Icon(
                            painter = painterResource(CoreR.drawable.ic_film),
                            contentDescription = null,
                        )
                    }
                }
                FilledTonalIconButton(onClick = onMarkAsPlayedClick) {
                    Icon(
                        painter = painterResource(CoreR.drawable.ic_check),
                        contentDescription = null,
                        tint = if (item.played) Color.Red else LocalContentColor.current,
                    )
                }
                FilledTonalIconButton(onClick = onMarkAsFavoriteClick) {
                    when (item.favorite) {
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
                if (downloaderState != null && !downloaderState.isDownloading) {
                    if (item.isDownloaded()) {
                        FilledTonalIconButton(onClick = { deleteDownloadDialogOpen = true }) {
                            Icon(
                                painter = painterResource(CoreR.drawable.ic_trash),
                                contentDescription = null,
                            )
                        }
                    } else if (item.canDownload && item.sources.indexOfFirst { it.size > 0 } > -1) {
                        FilledTonalIconButton(
                            onClick = {
                                mediaActionKind = MediaActionKind.DOWNLOAD
                                storageLocations = context.getExternalFilesDirs(null)
                                if (storageLocations.size > 1) {
                                    storageSelectionDialogOpen = true
                                } else {
                                    selectedStorageIndex = 0
                                    showMediaSourceSelectorDialog = true
                                }
                            }
                        ) {
                            Icon(
                                painter = painterResource(CoreR.drawable.ic_download),
                                contentDescription = null,
                            )
                        }
                    }
                }
            }
            if (downloaderState != null) {
                AnimatedVisibility(downloaderState.isDownloading) {
                    Column {
                        DownloaderCard(
                            state = downloaderState,
                            onCancelClick = { cancelDownloadDialogOpen = true },
                            onRetryClick = {
                                onDownloadClick(Pair(selectedStorageIndex, selectedMediaSourceId))
                            },
                        )
                        Spacer(Modifier.height(MaterialTheme.spacings.small))
                    }
                }
            }
        }
        if (item.sources.size > 1) {
            hasMultipleMediaSources = true
            for (source in item.sources) {
                mediaSources.remove(Pair(source.id, source.name))
                mediaSources.add(Pair(source.id, source.name))
            }
        }
        if (storageSelectionDialogOpen) {
            val locations = remember {
                storageLocations.map { dir ->
                    val locationStringRes =
                        if (Environment.isExternalStorageRemovable(dir)) CoreR.string.external
                        else CoreR.string.internal
                    val locationString = context.getString(locationStringRes)

                    val stat = StatFs(dir.path)
                    val availableMegaBytes = stat.availableBytes.div(1000000)
                    context.getString(CoreR.string.storage_name, locationString, availableMegaBytes)
                }
            }
            StorageSelectionDialog(
                storageLocations = locations,
                onSelect = { storageIndex ->
                    selectedStorageIndex = storageIndex
                    showMediaSourceSelectorDialog = true
                    storageSelectionDialogOpen = false
                },
                onDismiss = { storageSelectionDialogOpen = false },
            )
        }
        if (cancelDownloadDialogOpen) {
            CancelDownloadDialog(
                onCancel = {
                    onDownloadCancelClick()
                    cancelDownloadDialogOpen = false
                },
                onDismiss = { cancelDownloadDialogOpen = false },
            )
        }
        if (deleteDownloadDialogOpen) {
            DeleteDownloadDialog(
                onDelete = {
                    onDownloadDeleteClick()
                    deleteDownloadDialogOpen = false
                },
                onDismiss = { deleteDownloadDialogOpen = false },
            )
        }
        if (showMediaSourceSelectorDialog) {
            if (hasMultipleMediaSources) {
                VersionSelectionDialog(
                    mediaSources = mediaSources,
                    onSelect = { mediaSourceId ->
                        selectedMediaSourceId = mediaSourceId
                        processMedaiAction = true
                        showMediaSourceSelectorDialog = false
                    },
                    onDismiss = { showMediaSourceSelectorDialog = false },
                )
            } else {
                selectedMediaSourceId = item.sources.first().id
                processMedaiAction = true
            }
        }
        if (processMedaiAction) {
            processMedaiAction = false
            mediaActionKind = MediaActionKind.NONE
            if (mediaActionKind == MediaActionKind.PLAY)
                onPlayClick(Pair(mediaStartFromBeginning, selectedMediaSourceId))
            else if (mediaActionKind == MediaActionKind.DOWNLOAD)
                onDownloadClick(Pair(selectedStorageIndex, selectedMediaSourceId))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ItemButtonsBarPreview() {
    FindroidTheme {
        ItemButtonsBar(
            item = dummyEpisode,
            onPlayClick = {},
            onMarkAsPlayedClick = {},
            onMarkAsFavoriteClick = {},
            onDownloadClick = {},
            onDownloadCancelClick = {},
            onDownloadDeleteClick = {},
            onTrailerClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ItemButtonsBarDownloadingPreview() {
    FindroidTheme {
        ItemButtonsBar(
            item = dummyEpisode,
            downloaderState =
                DownloaderState(status = DownloadManager.STATUS_RUNNING, progress = 0.3f),
            onPlayClick = {},
            onMarkAsPlayedClick = {},
            onMarkAsFavoriteClick = {},
            onDownloadClick = {},
            onDownloadCancelClick = {},
            onDownloadDeleteClick = {},
            onTrailerClick = {},
        )
    }
}
