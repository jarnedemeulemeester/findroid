package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jdtech.jellyfin.core.presentation.dummy.dummyEpisode
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.FindroidMovie
import dev.jdtech.jellyfin.models.FindroidShow
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.core.R as CoreR

@Composable
fun ItemButtonsBar(
    item: FindroidItem,
    onPlayClick: (startFromBeginning: Boolean) -> Unit,
    onMarkAsPlayedClick: () -> Unit,
    onMarkAsFavoriteClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onTrailerClick: (uri: String) -> Unit,
    modifier: Modifier = Modifier,
    isLoadingPlayer: Boolean = false,
    isLoadingRestartPlayer: Boolean = false,
) {
    val trailerUri = when (item) {
        is FindroidMovie -> {
            item.trailer
        }
        is FindroidShow -> {
            item.trailer
        }
        else -> null
    }

    Row(
        modifier = modifier,
    ) {
        PlayButton(
            item = item,
            onClick = {
                onPlayClick(false)
            },
            enabled = !isLoadingPlayer && !isLoadingRestartPlayer,
            isLoading = isLoadingPlayer,
        )
        if (item.playbackPositionTicks.div(600000000) > 0) {
            FilledTonalIconButton(
                onClick = {
                    onPlayClick(true)
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
        if (item.canDownload) {
            FilledTonalIconButton(
                onClick = onDownloadClick,
                enabled = false,
            ) {
                Icon(
                    painter = painterResource(CoreR.drawable.ic_download),
                    contentDescription = null,
                )
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
            onPlayClick = {},
            onMarkAsPlayedClick = {},
            onMarkAsFavoriteClick = {},
            onDownloadClick = {},
            onTrailerClick = {},
        )
    }
}
