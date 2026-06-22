package dev.jdtech.jellyfin.presentation.cast.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import dev.jdtech.jellyfin.player.core.domain.models.PlayerChapter
import dev.jdtech.jellyfin.player.core.domain.models.Trickplay
import dev.jdtech.jellyfin.player.core.domain.utils.TimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CastScrubbingTimeline(
    currentProgress: Float,
    duration: Float,
    chapters: List<PlayerChapter>,
    onScrubStart: (Float) -> Unit,
    onScrubStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(horizontal = 24.dp)) {
        Slider(
            value = currentProgress.coerceIn(0f, duration.coerceAtLeast(1f)),
            onValueChange = { 
                onScrubStart(it) 
            },
            onValueChangeFinished = onScrubStop,
            valueRange = 0f..duration.coerceAtLeast(1f),
            modifier = Modifier.fillMaxWidth(),
            track = { sliderState ->
                SliderDefaults.Track(
                    sliderState = sliderState,
                    drawStopIndicator = {
                        chapters.forEach { chapter ->
                            val fraction = chapter.startPosition.toFloat() / duration.coerceAtLeast(1f)
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
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(TimeUtils.formatTime(currentProgress.toLong()), style = MaterialTheme.typography.bodySmall)
            Text(TimeUtils.formatTime(duration.toLong()), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun TrickplayThumbnail(
    trickplay: Trickplay,
    scrubPosition: Float,
    modifier: Modifier = Modifier
) {
    val index = (scrubPosition / trickplay.interval).toInt().coerceIn(0, (trickplay.images.size - 1))
    val image = trickplay.images[index]

    Image(
        bitmap = image.asImageBitmap(),
        contentDescription = "Scrub Preview",
        contentScale = ContentScale.Crop,
        modifier = modifier.fillMaxSize()
    )
}


