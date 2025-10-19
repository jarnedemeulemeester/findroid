package dev.jdtech.jellyfin.presentation.film.components

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
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
import dev.jdtech.jellyfin.core.presentation.dummy.dummyEpisode
import dev.jdtech.jellyfin.dlna.DlnaHelper
import dev.jdtech.jellyfin.roku.RokuHelper
import dev.jdtech.jellyfin.models.FindroidItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import dev.jdtech.jellyfin.models.FindroidMovie
import dev.jdtech.jellyfin.models.FindroidShow
import dev.jdtech.jellyfin.models.isDownloaded
import dev.jdtech.jellyfin.models.isDownloading
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.core.R as CoreR

fun isDlnaEnabled(context: Context): Boolean {
    val prefsName = context.packageName + "_preferences"
    val sharedPreferences = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
    return sharedPreferences.getBoolean("pref_dlna_enabled", true)
}

@Composable
fun ItemButtonsBar(
    item: FindroidItem,
    onPlayClick: (startFromBeginning: Boolean) -> Unit,
    onMarkAsPlayedClick: () -> Unit,
    onMarkAsFavoriteClick: () -> Unit,
    onDlnaClick: () -> Unit = {},
    onDownloadClick: () -> Unit,
    onDeleteClick: (() -> Unit)? = null,
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
        is dev.jdtech.jellyfin.models.FindroidEpisode -> {
            item.trailer
        }
        else -> null
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
    ) {
        Row {
            PlayButton(
                item = item,
                onClick = {
                    onPlayClick(false)
                },
                modifier = Modifier.weight(
                    weight = 1f,
                    fill = !windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND),
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
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
            ) {
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
                
                // DLNA button - only show if enabled in settings
                val context = LocalContext.current
                
                if (isDlnaEnabled(context)) {
                    var isDlnaActive by remember { mutableStateOf(false) }
                    var isRokuActive by remember { mutableStateOf(false) }
                    
                    // Update DLNA and Roku active state periodically
                    DisposableEffect(Unit) {
                        var updateJob: Job? = null
                        
                        updateJob = CoroutineScope(Dispatchers.Main).launch {
                            while (isActive) {
                                isDlnaActive = DlnaHelper.isDlnaDeviceAvailable(context)
                                isRokuActive = RokuHelper.isRokuDeviceAvailable()
                                delay(500) // Check every 500ms
                            }
                        }
                        
                        onDispose {
                            updateJob?.cancel()
                        }
                    }
                    
                    // Show filled button if DLNA or Roku is active, otherwise tonal button
                    if (isDlnaActive || isRokuActive) {
                        // Filled button when DLNA or Roku is active
                        FilledIconButton(
                            onClick = onDlnaClick,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(
                                painter = painterResource(CoreR.drawable.ic_tv),
                                contentDescription = "DLNA/Roku Active",
                            )
                        }
                    } else {
                        // Tonal button when both are inactive
                        FilledTonalIconButton(
                            onClick = onDlnaClick,
                        ) {
                            Icon(
                                painter = painterResource(CoreR.drawable.ic_tv),
                                contentDescription = "DLNA/Roku",
                            )
                        }
                    }
                }
                
                // Download button only when allowed; delete button always when downloaded
                if (item.canDownload) {
                    val downloadEnabled = !item.isDownloaded() && !item.isDownloading()
                    FilledTonalIconButton(
                        onClick = onDownloadClick,
                        enabled = downloadEnabled,
                    ) {
                        Icon(
                            painter = painterResource(CoreR.drawable.ic_download),
                            contentDescription = null,
                        )
                    }
                }
                if (item.isDownloaded() && onDeleteClick != null) {
                    FilledTonalIconButton(
                        onClick = onDeleteClick,
                    ) {
                        Icon(
                            painter = painterResource(CoreR.drawable.ic_trash),
                            contentDescription = null,
                        )
                    }
                }
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
            onDeleteClick = {},
            onTrailerClick = {},
        )
    }
}
