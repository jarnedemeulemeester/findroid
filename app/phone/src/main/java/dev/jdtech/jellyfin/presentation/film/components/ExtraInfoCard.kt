package dev.jdtech.jellyfin.presentation.film.components

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jdtech.jellyfin.core.presentation.dummy.dummyVideoMetadata
import dev.jdtech.jellyfin.models.FindroidImages
import dev.jdtech.jellyfin.models.FindroidPart
import dev.jdtech.jellyfin.models.FindroidSource
import dev.jdtech.jellyfin.models.FindroidSourceType
import dev.jdtech.jellyfin.models.VideoMetadata
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.utils.getTranslatablePartName
import java.util.UUID
import dev.jdtech.jellyfin.core.R as CoreR

@Composable
fun ExtraInfoCard(
    videoMetadata: VideoMetadata,
    additionalParts: List<FindroidPart> = emptyList()
) {
    val context = LocalContext.current

    val mainSize = videoMetadata.size
    val partsSizes = additionalParts.map { it.sources.firstOrNull()?.size ?: 0L }
    val totalSize = mainSize + partsSizes.sum()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column {
            Row(
                modifier = Modifier.padding(MaterialTheme.spacings.medium),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small)
            ) {
                Icon(
                    painter = painterResource(CoreR.drawable.ic_info),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(CoreR.string.info),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )

            Column(
                modifier = Modifier.padding(MaterialTheme.spacings.medium),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small)
            ) {
                val totalSizeText = Formatter.formatFileSize(context, totalSize)

                val firstPartName = if (additionalParts.isNotEmpty()) {
                    val firstPartType = additionalParts.firstOrNull()?.name?.let {
                        Regex("(?i)(cd|dvd|part|pt|disc|disk)").find(it)?.groupValues?.get(1)
                            ?.lowercase()
                    }
                    when (firstPartType) {
                        "cd" -> stringResource(CoreR.string.cd_name, "1")
                        "dvd" -> stringResource(CoreR.string.dvd_name, "1")
                        "disc", "disk" -> stringResource(CoreR.string.disc_name, "1")
                        else -> stringResource(CoreR.string.part_name, "1")
                    }
                } else null

                InfoRow(stringResource(CoreR.string.size)) {
                    Text(text = totalSizeText, style = MaterialTheme.typography.bodyMedium)
                    if (additionalParts.isNotEmpty() && firstPartName != null) {
                        Text(
                            text = "${Formatter.formatFileSize(context, mainSize)} ($firstPartName)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        additionalParts.forEach { part ->
                            val partSize = part.sources.firstOrNull()?.size ?: 0L
                            Text(
                                text = "${Formatter.formatFileSize(context, partSize)} (${part.name.getTranslatablePartName(context)})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                if (videoMetadata.videoTracks.isNotEmpty()) {
                    InfoRow(stringResource(CoreR.string.video)) {
                        videoMetadata.videoTracks.forEach { track ->
                            Text(text = track, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                if (videoMetadata.audioTracks.isNotEmpty()) {
                    InfoRow(stringResource(CoreR.string.audio)) {
                        videoMetadata.audioTracks.forEach { track ->
                            Text(text = track, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                if (videoMetadata.subtitleTracks.isNotEmpty()) {
                    InfoRow(stringResource(CoreR.string.subtitle)) {
                        videoMetadata.subtitleTracks.forEach { track ->
                            Text(text = track, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(tag: String, content: @Composable ColumnScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = tag,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Start,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(MaterialTheme.spacings.medium))
        Column(
            horizontalAlignment = Alignment.End
        ) {
            content()
        }
    }
}

@Preview
@Composable
private fun ExtraInfoCardPreview() {
    FindroidTheme {
        ExtraInfoCard(
            videoMetadata = dummyVideoMetadata.copy(
                videoTracks = listOf("1080p H264 SDR"),
                audioTracks = listOf("English - AC3 - 5.1", "Italian - AAC - Stereo"),
                subtitleTracks = listOf("English", "Italian", "Spanish")
            )
        )
    }
}

@Preview
@Composable
private fun ExtraInfoCardMultiPartPreview() {
    FindroidTheme {
        ExtraInfoCard(
            videoMetadata = dummyVideoMetadata.copy(
                size = 1200000000,
                videoTracks = listOf("1080p HEVC HDR10"),
                audioTracks = listOf("English - TRUEHD - 7.1"),
                subtitleTracks = listOf("English (Forced)", "English")
            ),
            additionalParts = listOf(
                FindroidPart(
                    id = UUID.randomUUID(),
                    name = "Part 2",
                    played = false,
                    favorite = false,
                    images = FindroidImages(),
                    sources = listOf(
                        FindroidSource(
                            id = "2",
                            name = "Part 2",
                            type = FindroidSourceType.REMOTE,
                            path = "",
                            size = 800000000,
                            mediaStreams = emptyList()
                        )
                    ),
                    runtimeTicks = 0,
                    playbackPositionTicks = 0
                )
            )
        )
    }
}
