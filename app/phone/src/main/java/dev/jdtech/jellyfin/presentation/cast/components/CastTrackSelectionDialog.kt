package dev.jdtech.jellyfin.presentation.cast.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.MimeTypes
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
                        TrackRow(
                            trackName = stringResource(R.string.none),
                            trackLanguage = null,
                            isSelected = tracks.none { it.selected }
                        ) {
                            onSetTrack(null)
                        }
                    }
                }
                items(tracks, key = { it.id }) { track ->
                    TrackRow(
                        trackName = track.label,
                        trackLanguage = track.language,
                        trackCodec = track.codec,
                        isSelected = track.selected
                    ) {
                        onSetTrack(track)
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackRow(
    trackName: String?,
    trackLanguage: String?,
    isSelected: Boolean,
    trackCodec: String? = null,
    onClick: () -> Unit
) {
    val noneString = stringResource(R.string.none)
    val (displayTrackName, filteredLabel) = remember(trackName, trackLanguage, trackCodec) {
        val locale = trackLanguage?.takeIf { it.isNotBlank() && it != "und" }?.let { lang ->
            Locale.forLanguageTag(lang.replace("_", "-"))
        }

        val localizedLanguage = locale?.displayLanguage?.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }

        val englishLanguage = locale?.getDisplayLanguage(Locale.ENGLISH)

        val filteredLabel = trackName?.takeIf {
            it.isNotBlank() &&
                    !it.equals(noneString, ignoreCase = true) &&
                    !it.equals(localizedLanguage, ignoreCase = true) &&
                    !it.equals(englishLanguage, ignoreCase = true) &&
                    !it.equals(trackLanguage, ignoreCase = true)
        }

        val displayName = localizedLanguage ?: trackName ?: ""

        displayName to filteredLabel
    }

    val trackCodecText = when (trackCodec) {
        MimeTypes.APPLICATION_SUBRIP -> "subrip"
        MimeTypes.TEXT_VTT -> "webvtt"
        MimeTypes.TEXT_SSA -> "ass"
        else -> null
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = isSelected, onClick = onClick)

        Spacer(modifier = Modifier.width(8.dp))

        Text(text = displayTrackName, style = MaterialTheme.typography.bodyMedium)

        Spacer(modifier = Modifier.width(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small)) {
            if (filteredLabel != null) {
                TrackMetadataBarItem(filteredLabel)
            }
            if (trackCodecText != null) {
                TrackMetadataBarItem(trackCodecText)
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
            onDismiss = {}
        )
    }
}
