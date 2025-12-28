package dev.jdtech.jellyfin.presentation.film.components

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.models.VideoMetadata
import dev.jdtech.jellyfin.presentation.theme.spacings

@Composable
fun ExtraInfoText(videoMetadata: VideoMetadata) {
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small)) {
        Text(
            text =
                "${stringResource(CoreR.string.size)}: ${Formatter.formatFileSize(context, videoMetadata.size)}",
            style = MaterialTheme.typography.bodyMedium,
        )
        if (videoMetadata.videoTracks.isNotEmpty()) {
            Text(
                text =
                    "${stringResource(CoreR.string.video)}: ${videoMetadata.videoTracks.joinToString { it }}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (videoMetadata.audioTracks.isNotEmpty()) {
            Text(
                text =
                    "${stringResource(CoreR.string.audio)}: ${videoMetadata.audioTracks.joinToString { it }}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (videoMetadata.subtitleTracks.isNotEmpty()) {
            Text(
                text =
                    "${stringResource(CoreR.string.subtitle)}: ${videoMetadata.subtitleTracks.joinToString { it }}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
