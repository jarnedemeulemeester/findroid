package dev.jdtech.jellyfin.presentation.film.components

import android.app.DownloadManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.core.presentation.downloader.DownloaderState
import dev.jdtech.jellyfin.core.presentation.dummy.dummyEpisode
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.FindroidMovie
import dev.jdtech.jellyfin.models.FindroidSeason
import dev.jdtech.jellyfin.models.FindroidShow
import dev.jdtech.jellyfin.models.isDownloaded
import dev.jdtech.jellyfin.offline.download.OfflineProfile
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings

@Composable
fun ItemButtonsBar(
    item: FindroidItem,
    onPlayClick: (startFromBeginning: Boolean) -> Unit,
    onMarkAsPlayedClick: () -> Unit,
    onMarkAsFavoriteClick: () -> Unit,
    onDownloadClick: (profile: OfflineProfile) -> Unit,
    onDownloadCancelClick: () -> Unit,
    onDownloadDeleteClick: () -> Unit,
    onTrailerClick: (uri: String) -> Unit,
    modifier: Modifier = Modifier,
    downloaderState: DownloaderState? = null,
    canPlay: Boolean = true,
) {
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

    var profileSelectionDialogOpen by remember { mutableStateOf(false) }
    var cancelDownloadDialogOpen by remember { mutableStateOf(false) }
    var deleteDownloadDialogOpen by remember { mutableStateOf(false) }

    var selectedProfile by remember { mutableStateOf(OfflineProfile.Default480p) }
    val isDownloaded =
        item.isDownloaded() || downloaderState?.status == DownloadManager.STATUS_SUCCESSFUL
    val canDownload = item.canDownload || item is FindroidShow || item is FindroidSeason

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
                        onClick = { onPlayClick(false) },
                        modifier = Modifier.weight(weight = 1f, fill = true),
                        enabled = item.canPlay && canPlay,
                    )
                    if (item.playbackPositionTicks.div(600000000) > 0) {
                        FilledTonalIconButton(onClick = { onPlayClick(true) }) {
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
                        onClick = { onPlayClick(false) },
                        enabled = item.canPlay && canPlay,
                    )
                    if (item.playbackPositionTicks.div(600000000) > 0) {
                        FilledTonalIconButton(onClick = { onPlayClick(true) }) {
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
                    if (isDownloaded) {
                        FilledTonalIconButton(onClick = { deleteDownloadDialogOpen = true }) {
                            Icon(
                                painter = painterResource(CoreR.drawable.ic_trash),
                                contentDescription = null,
                            )
                        }
                    } else if (canDownload) {
                        FilledTonalIconButton(
                            onClick = {
                                profileSelectionDialogOpen = true
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
                            onRetryClick = { onDownloadClick(selectedProfile) },
                        )
                        Spacer(Modifier.height(MaterialTheme.spacings.small))
                    }
                }
            }
        }
        if (profileSelectionDialogOpen) {
            DownloadProfileDialog(
                onSelect = { profile ->
                    selectedProfile = profile
                    onDownloadClick(profile)
                    profileSelectionDialogOpen = false
                },
                onDismiss = { profileSelectionDialogOpen = false },
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
    }
}

@Composable
private fun DownloadProfileDialog(
    onSelect: (OfflineProfile) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Download quality") },
        text = { Text(text = "Default is optimized for Android Auto and phone storage.") },
        confirmButton = {
            TextButton(onClick = { onSelect(OfflineProfile.Default480p) }) {
                Text(text = "480p default")
            }
        },
        dismissButton = {
            TextButton(onClick = { onSelect(OfflineProfile.High720p) }) { Text(text = "720p high") }
        },
    )
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
