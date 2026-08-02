package dev.jdtech.jellyfin.presentation.cast.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import dev.jdtech.jellyfin.player.core.R
import dev.jdtech.jellyfin.player.core.domain.models.Track
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun CastTrackSelectionSheet(
    type: @C.TrackType Int,
    tracks: List<Track>,
    onSetTrack: (Track?) -> Unit,
    onDismiss: () -> Unit,
    displayExtraInfo: Boolean,
    modifier: Modifier = Modifier
) {
    var offsetY by remember { mutableFloatStateOf(0f) }
    val animatedOffset by animateFloatAsState(targetValue = offsetY, label = "offset")

    val titleResource =
        when (type) {
            C.TRACK_TYPE_AUDIO -> R.string.select_audio_track
            C.TRACK_TYPE_TEXT -> R.string.select_subtitle_track
            else -> throw IllegalStateException("TrackType must be AUDIO or TEXT")
        }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .offset { IntOffset(0, animatedOffset.roundToInt()) }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (offsetY > 150f) {
                            onDismiss()
                        }
                        offsetY = 0f
                    },
                    onVerticalDrag = { _, dragAmount ->
                        val newOffset = offsetY + dragAmount
                        offsetY = newOffset.coerceAtLeast(0f)
                    }
                )
            },
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .padding(MaterialTheme.spacings.medium)
                .heightIn(max = 450.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(32.dp)
                    .height(4.dp)
                    .background(
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        CircleShape
                    )
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacings.small))

            Text(
                text = stringResource(titleResource),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(8.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1f, fill = false)
            ) {
                if (type == C.TRACK_TYPE_TEXT) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = { onSetTrack(null) })
                                .padding(vertical = 4.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = tracks.none { it.selected },
                                onClick = { onSetTrack(null) })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.none),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                items(tracks, key = { it.id }) { track ->
                    TrackRow(
                        track = track,
                        displayExtraInfo = displayExtraInfo,
                        onClick = { onSetTrack(track) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackRow(
    track: Track,
    displayExtraInfo: Boolean,
    onClick: (Track) -> Unit
) {
    val configuration = LocalConfiguration.current
    val currentLocale = configuration.locales[0]

    val trackIndexName = stringResource(R.string.track_index, track.id)

    val displayName = remember(track, currentLocale) {
        val locale = track.language?.takeIf { it.isNotBlank() && it != "und" }?.let { lang ->
            Locale.forLanguageTag(lang.replace("_", "-"))
        }

        val localizedLanguage = locale?.getDisplayLanguage(currentLocale)?.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(currentLocale) else it.toString()
        }

        localizedLanguage ?: track.label ?: trackIndexName
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = { onClick(track) })
            .padding(vertical = 4.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = track.selected, onClick = { onClick(track) })

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = displayName,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1
        )

        Spacer(modifier = Modifier.width(16.dp))

        val scrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .weight(1f)
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .drawWithContent {
                    drawContent()
                    val edgeWidth = 16.dp.toPx()

                    if (scrollState.value > 0) {
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color.Transparent, Color.Black),
                                startX = 0f,
                                endX = edgeWidth
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    }

                    if (scrollState.value < scrollState.maxValue) {
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color.Black, Color.Transparent),
                                startX = size.width - edgeWidth,
                                endX = size.width
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    }
                }
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (track.isForced == true) {
                TrackMetadataBarItem(stringResource(R.string.forced))
            }
            if (track.isHearingImpaired == true) {
                TrackMetadataBarItem(stringResource(R.string.hearing_impaired))
            }
            if (displayExtraInfo) {
                if (track.isExternal == true) {
                    TrackMetadataBarItem(stringResource(R.string.external))
                }
                track.codec?.takeIf { it.isNotBlank() }?.let {
                    TrackMetadataBarItem(it)
                }
            }
        }
    }
}

@Composable
fun TrackMetadataBarItem(trackSpec: String, @DrawableRes icon: Int? = null) {
    Row(
        modifier =
            Modifier
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(
                    horizontal = MaterialTheme.spacings.small,
                    vertical = MaterialTheme.spacings.extraSmall,
                ),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.extraSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(painter = painterResource(icon), contentDescription = null)
        }
        Text(text = trackSpec, style = MaterialTheme.typography.labelMedium)
    }
}

@Preview
@Composable
private fun CastTrackSelectionSheetPreview() {
    FindroidTheme {
        CastTrackSelectionSheet(
            type = C.TRACK_TYPE_AUDIO,
            tracks = listOf(
                Track(3, "English", "eng", "aac", selected = false, supported = true),
                Track(4, "Spanish", "spa", "aac", selected = true, supported = true)
            ),
            onSetTrack = {},
            onDismiss = {},
            displayExtraInfo = false
        )
    }
}

@Preview
@Composable
private fun CastTrackSubsSelectionSheetPreview() {
    FindroidTheme {
        CastTrackSelectionSheet(
            type = C.TRACK_TYPE_TEXT,
            tracks = listOf(
                Track(
                    3,
                    "English",
                    "eng",
                    "subrip",
                    selected = false,
                    supported = true,
                    isForced = true
                ),
                Track(
                    4,
                    "Spanish",
                    "spa",
                    "webvvt",
                    selected = true,
                    supported = true,
                    isHearingImpaired = true,
                    isExternal = true
                ),
                Track(
                    5,
                    null,
                    null,
                    "webvvt",
                    selected = false,
                    supported = true,
                    isHearingImpaired = false,
                    isExternal = false
                )
            ),
            onSetTrack = {},
            onDismiss = {},
            displayExtraInfo = true
        )
    }
}