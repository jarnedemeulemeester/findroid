package dev.jdtech.jellyfin.presentation.cast.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.core.R as CoreR

@Composable
fun CastVolumeControls(
    volume: Float,
    onVolumeChange: (Float) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = MaterialTheme.spacings.extraLarge),
        verticalAlignment = Alignment.CenterVertically
    ) {
        VolumeSlider(
            volume = volume,
            onValueChange = onVolumeChange,
            modifier = Modifier.weight(1f)
        )
    }
}

private object VolumeSliderDefaults {
    val TrackHeight: Dp = 40.dp
    val HandleHeight: Dp = 52.dp
    val HandleWidth: Dp = 4.dp
    val TrackCornerRadius: Dp = 12.dp
    val InsetIconSize: Dp = 24.dp
    val IconPadding: Dp = 10.dp
    val ThumbTrackGapSize: Dp = 6.dp
    val StopIndicatorRadius: Dp = 4.dp
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VolumeSlider(
    volume: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var localVolume by remember { mutableFloatStateOf(volume) }

    LaunchedEffect(volume) {
        localVolume = volume
    }

    val currentIcon = when {
        localVolume <= 0f -> painterResource(CoreR.drawable.ic_volume_0)
        localVolume < 0.33f -> painterResource(CoreR.drawable.ic_volume_33)
        localVolume < 0.66f -> painterResource(CoreR.drawable.ic_volume_66)
        else -> painterResource(CoreR.drawable.ic_volume_100)
    }

    val colors = SliderDefaults.colors(
        activeTrackColor = MaterialTheme.colorScheme.primary,
        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
        thumbColor = MaterialTheme.colorScheme.primary
    )

    Slider(
        value = localVolume,
        onValueChange = {
            localVolume = it
        },
        onValueChangeFinished = {
            onValueChange(localVolume)
        },
        modifier = modifier,
        valueRange = 0f..1f,
        colors = colors,
        track = { sliderState ->
            val iconSize = DpSize(VolumeSliderDefaults.InsetIconSize, VolumeSliderDefaults.InsetIconSize)
            val activeIconColor = MaterialTheme.colorScheme.onPrimary
            val inactiveIconColor = MaterialTheme.colorScheme.onSurfaceVariant

            SliderDefaults.Track(
                sliderState = sliderState,
                modifier = Modifier
                    .height(VolumeSliderDefaults.TrackHeight)
                    .drawWithContent {
                        drawContent()
                        val yOffset = size.height / 2 - iconSize.toSize().height / 2
                        val fraction = localVolume.coerceIn(0f, 1f)
                        val thumbGapPx = VolumeSliderDefaults.ThumbTrackGapSize.toPx()
                        val activeTrackEnd = size.width * fraction - thumbGapPx
                        val inactiveTrackStart = activeTrackEnd + thumbGapPx * 2
                        val inactiveTrackWidth = size.width - inactiveTrackStart

                        drawVolumeIcon(
                            icon = currentIcon,
                            iconSize = iconSize,
                            iconPadding = VolumeSliderDefaults.IconPadding,
                            yOffset = yOffset,
                            activeTrackWidth = activeTrackEnd,
                            inactiveTrackStart = inactiveTrackStart,
                            inactiveTrackWidth = inactiveTrackWidth,
                            activeIconColor = activeIconColor,
                            inactiveIconColor = inactiveIconColor
                        )
                    },
                colors = colors,
                enabled = true,
                thumbTrackGapSize = VolumeSliderDefaults.ThumbTrackGapSize,
                drawStopIndicator = null
            )
        }
    )
}

private fun DrawScope.drawVolumeIcon(
    icon: Painter,
    iconSize: DpSize,
    iconPadding: Dp,
    yOffset: Float,
    activeTrackWidth: Float,
    inactiveTrackStart: Float,
    inactiveTrackWidth: Float,
    activeIconColor: Color,
    inactiveIconColor: Color
) {
    val iconSizePx = iconSize.toSize()
    val iconPaddingPx = iconPadding.toPx()
    val minSpaceForIcon = iconSizePx.width + iconPaddingPx * 2

    if (activeTrackWidth >= minSpaceForIcon) {
        translate(iconPaddingPx, yOffset) {
            with(icon) {
                draw(iconSizePx, colorFilter = ColorFilter.tint(activeIconColor))
            }
        }
    } else if (inactiveTrackWidth >= minSpaceForIcon) {
        translate(inactiveTrackStart + iconPaddingPx, yOffset) {
            with(icon) {
                draw(iconSizePx, colorFilter = ColorFilter.tint(inactiveIconColor))
            }
        }
    }
}
