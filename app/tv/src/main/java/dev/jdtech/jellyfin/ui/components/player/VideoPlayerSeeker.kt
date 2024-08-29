package dev.jdtech.jellyfin.ui.components.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.media3.common.C
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jdtech.jellyfin.ui.theme.FindroidTheme
import dev.jdtech.jellyfin.ui.theme.spacings
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import dev.jdtech.jellyfin.core.R as CoreR

@Composable
fun VideoPlayerSeeker(
    focusRequester: FocusRequester,
    state: VideoPlayerState,
    isPlaying: Boolean,
    onPlayPauseToggle: (Boolean) -> Unit,
    onSeek: (Float) -> Unit,
    contentProgress: Duration,
    contentDuration: Duration,
    seekBackIncrement: Duration,
    seekForwardIncrement: Duration,
) {
    val contentProgressString =
        contentProgress.toComponents { h, m, s, _ ->
            if (h > 0) {
                "$h:${m.padStartWith0()}:${s.padStartWith0()}"
            } else {
                "${m.padStartWith0()}:${s.padStartWith0()}"
            }
        }
    val contentDurationString =
        contentDuration.toComponents { h, m, s, _ ->
            if (h > 0) {
                "$h:${m.padStartWith0()}:${s.padStartWith0()}"
            } else {
                "${m.padStartWith0()}:${s.padStartWith0()}"
            }
        }

    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = {
                onPlayPauseToggle(!isPlaying)
            },
            modifier = Modifier.focusRequester(focusRequester),
        ) {
            if (!isPlaying) {
                Icon(
                    painter = painterResource(id = CoreR.drawable.ic_play),
                    contentDescription = null,
                )
            } else {
                Icon(
                    painter = painterResource(id = CoreR.drawable.ic_pause),
                    contentDescription = null,
                )
            }
        }
        Spacer(modifier = Modifier.width(MaterialTheme.spacings.medium))
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = contentProgressString,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                )
                Text(
                    text = contentDurationString,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                )
            }
            Spacer(modifier = Modifier.height(MaterialTheme.spacings.small))
            VideoPlayerSeekBar(
                contentProgress = contentProgress,
                contentDuration = contentDuration,
                seekBackIncrement = seekBackIncrement,
                seekForwardIncrement = seekForwardIncrement,
                onSeek = onSeek,
                state = state,
            )
        }
    }
}

@Preview
@Composable
private fun VideoPlayerSeekerPreview() {
    FindroidTheme {
        VideoPlayerSeeker(
            focusRequester = FocusRequester(),
            state = rememberVideoPlayerState(),
            isPlaying = false,
            onPlayPauseToggle = {},
            onSeek = {},
            contentProgress = Duration.parse("7m 51s"),
            contentDuration = Duration.parse("23m 40s"),
            seekBackIncrement = C.DEFAULT_SEEK_BACK_INCREMENT_MS.milliseconds,
            seekForwardIncrement = C.DEFAULT_SEEK_FORWARD_INCREMENT_MS.milliseconds,
        )
    }
}

private fun Number.padStartWith0() = this.toString().padStart(2, '0')
