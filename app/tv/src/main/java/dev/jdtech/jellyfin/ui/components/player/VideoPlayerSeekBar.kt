package dev.jdtech.jellyfin.ui.components.player

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.utils.handleDPadKeyEvents

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun VideoPlayerSeekBar(
    progress: Float,
    onSeek: (seekProgress: Float) -> Unit,
    state: VideoPlayerState,
) {
    val interactionSource = remember { MutableInteractionSource() }
    var isSelected by remember { mutableStateOf(false) }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val color by rememberUpdatedState(
        newValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurface
        },
    )
    val animatedHeight by animateDpAsState(
        targetValue = 8.dp.times(if (isFocused) 2f else 1f),
    )
    var seekProgress by remember { mutableFloatStateOf(0f) }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(isSelected) {
        if (isSelected) {
            state.showControls(seconds = Int.MAX_VALUE)
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(animatedHeight)
            .padding(horizontal = 4.dp)
            .handleDPadKeyEvents(
                onEnter = {
                    if (isSelected) {
                        onSeek(seekProgress)
                        focusManager.moveFocus(FocusDirection.Exit)
                    } else {
                        seekProgress = progress
                    }
                    isSelected = !isSelected
                },
                onLeft = {
                    if (isSelected) {
                        seekProgress = (seekProgress - 0.05f).coerceAtLeast(0f)
                    } else {
                        focusManager.moveFocus(FocusDirection.Left)
                    }
                },
                onRight = {
                    if (isSelected) {
                        seekProgress = (seekProgress + 0.05f).coerceAtMost(1f)
                    } else {
                        focusManager.moveFocus(FocusDirection.Right)
                    }
                },
            )
            .focusable(interactionSource = interactionSource),
    ) {
        val yOffset = size.height.div(2)
        drawLine(
            color = color.copy(alpha = 0.24f),
            start = Offset(x = 0f, y = yOffset),
            end = Offset(x = size.width, y = yOffset),
            strokeWidth = size.height.div(2),
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(x = 0f, y = yOffset),
            end = Offset(
                x = size.width.times(if (isSelected) seekProgress else progress),
                y = yOffset,
            ),
            strokeWidth = size.height.div(2),
            cap = StrokeCap.Round,
        )
        drawCircle(
            color = Color.White,
            radius = size.height.div(2),
            center = Offset(
                x = size.width.times(if (isSelected) seekProgress else progress),
                y = yOffset,
            ),
        )
    }
}

@Preview
@Composable
fun VideoPlayerSeekBarPreview() {
    FindroidTheme {
        VideoPlayerSeekBar(
            progress = 0.4f,
            onSeek = {},
            state = rememberVideoPlayerState(),
        )
    }
}
