package dev.jdtech.jellyfin.presentation.cast.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jdtech.jellyfin.player.cast.models.CastPlaybackStatus
import dev.jdtech.jellyfin.player.core.domain.models.PlayerChapter
import dev.jdtech.jellyfin.player.core.domain.models.Trickplay
import dev.jdtech.jellyfin.player.core.domain.utils.TimeUtils
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CastScrubbingTimeline(
    playerStatus: CastPlaybackStatus,
    currentProgress: Float,
    duration: Float,
    chapters: List<PlayerChapter>,
    onScrubStart: (Float) -> Unit,
    onScrubStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(horizontal = 24.dp)) {
        if (playerStatus == CastPlaybackStatus.BUFFERING) {
            Box(modifier = Modifier.padding(horizontal = 2.dp, vertical = 16.dp)) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                )
            }
        } else {
            Slider(
                value = currentProgress.coerceIn(0f, duration.coerceAtLeast(1f)),
                onValueChange = {
                    onScrubStart(it)
                },
                onValueChangeFinished = onScrubStop,
                valueRange = 0f..duration.coerceAtLeast(1f),
                modifier = Modifier.fillMaxWidth(),
                enabled = duration > 0f,
                track = { sliderState ->
                    SliderDefaults.Track(
                        sliderState = sliderState,
                        drawStopIndicator = {
                            chapters.forEach { chapter ->
                                val fraction =
                                    chapter.startPosition.toFloat() / duration.coerceAtLeast(1f)
                                if (fraction in 0.01f..0.99f) {
                                    drawCircle(
                                        color = Color.White,
                                        radius = 2.dp.toPx(),
                                        center = Offset(size.width * fraction, size.height / 2)
                                    )
                                }
                            }
                        }
                    )
                }
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .alpha(if (duration > 0) 1f else 0f),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                TimeUtils.formatTime(currentProgress.toLong()),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                TimeUtils.formatTime(duration.toLong()),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun TrickplayThumbnail(
    trickplay: Trickplay,
    scrubPosition: Float,
    modifier: Modifier = Modifier
) {
    val index =
        (scrubPosition / trickplay.interval).toInt().coerceIn(0, (trickplay.images.size - 1))
    val image = trickplay.images[index]

    Image(
        bitmap = image.asImageBitmap(),
        contentDescription = "Scrub Preview",
        contentScale = ContentScale.Crop,
        modifier = modifier.fillMaxSize()
    )
}

@Preview(showBackground = true)
@Composable
private fun CastScrubbingTimelinePreview() {
    FindroidTheme {
        CastScrubbingTimeline(
            playerStatus = CastPlaybackStatus.PLAYING,
            currentProgress = 120000f,
            duration = 600000f,
            chapters = listOf(
                PlayerChapter(0L, "Chapter 1"),
                PlayerChapter(300000L, "Chapter 2")
            ),
            onScrubStart = {},
            onScrubStop = {}
        )
    }
}
