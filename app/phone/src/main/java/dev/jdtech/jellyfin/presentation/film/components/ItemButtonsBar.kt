package dev.jdtech.jellyfin.presentation.film.components

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import dev.jdtech.jellyfin.core.presentation.downloader.DownloaderState
import dev.jdtech.jellyfin.core.presentation.dummy.dummyEpisode
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.FindroidMovie
import dev.jdtech.jellyfin.models.FindroidShow
import dev.jdtech.jellyfin.models.isDownloaded
import dev.jdtech.jellyfin.models.isDownloading
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.core.R as CoreR

@Composable
fun ItemButtonsBar(
    item: FindroidItem,
    downloaderState: DownloaderState,
    onPlayClick: (startFromBeginning: Boolean) -> Unit,
    onMarkAsPlayedClick: () -> Unit,
    onMarkAsFavoriteClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onDownloadCancelClick: () -> Unit,
    onDownloadDeleteClick: () -> Unit,
    onTrailerClick: (uri: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass

    val trailerUri = when (item) {
        is FindroidMovie -> {
            item.trailer
        }
        is FindroidShow -> {
            item.trailer
        }
        else -> null
    }

    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
        ) {
            if (!windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
                ) {
                    PlayButton(
                        item = item,
                        onClick = {
                            onPlayClick(false)
                        },
                        modifier = Modifier.weight(
                            weight = 1f,
                            fill = true,
                        ),
                    )
                    if (item.playbackPositionTicks.div(600000000) > 0) {
                        FilledTonalIconButton(
                            onClick = {
                                onPlayClick(true)
                            },
                        ) {
                            Icon(
                                painter = painterResource(CoreR.drawable.ic_rotate_ccw),
                                contentDescription = null,
                            )
                        }
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
            ) {
                if (windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)) {
                    PlayButton(
                        item = item,
                        onClick = {
                            onPlayClick(false)
                        },
                    )
                    if (item.playbackPositionTicks.div(600000000) > 0) {
                        FilledTonalIconButton(
                            onClick = {
                                onPlayClick(true)
                            },
                        ) {
                            Icon(
                                painter = painterResource(CoreR.drawable.ic_rotate_ccw),
                                contentDescription = null,
                            )
                        }
                    }
                }
                trailerUri?.let { uri ->
                    FilledTonalIconButton(
                        onClick = {
                            onTrailerClick(uri)
                        },
                    ) {
                        Icon(
                            painter = painterResource(CoreR.drawable.ic_film),
                            contentDescription = null,
                        )
                    }
                }
                FilledTonalIconButton(
                    onClick = onMarkAsPlayedClick,
                ) {
                    Icon(
                        painter = painterResource(CoreR.drawable.ic_check),
                        contentDescription = null,
                        tint = if (item.played) Color.Red else LocalContentColor.current,
                    )
                }
                FilledTonalIconButton(
                    onClick = onMarkAsFavoriteClick,
                ) {
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
                if (!downloaderState.isDownloading) {
                    if (item.isDownloaded()) {
                        FilledTonalIconButton(
                            onClick = onDownloadDeleteClick,
                        ) {
                            Icon(
                                painter = painterResource(CoreR.drawable.ic_trash),
                                contentDescription = null,
                            )
                        }
                    } else if (item.canDownload) {
                        FilledTonalIconButton(
                            onClick = onDownloadClick,
                        ) {
                            Icon(
                                painter = painterResource(CoreR.drawable.ic_download),
                                contentDescription = null,
                            )
                        }
                    }
                }
            }
        }
        AnimatedVisibility(downloaderState.isDownloading) {
            Column {
                Spacer(Modifier.height(MaterialTheme.spacings.small))
                DownloaderCard(
                    state = downloaderState,
                    onCancelClick = onDownloadCancelClick,
                )
                Spacer(Modifier.height(MaterialTheme.spacings.small))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ItemButtonsBarPreview() {
    FindroidTheme {
        ItemButtonsBar(
            item = dummyEpisode,
            downloaderState = DownloaderState(),
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
