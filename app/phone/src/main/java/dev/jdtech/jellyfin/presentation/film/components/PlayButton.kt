package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jdtech.jellyfin.core.presentation.dummy.dummyEpisode
import dev.jdtech.jellyfin.core.presentation.dummy.dummyMovie
import dev.jdtech.jellyfin.models.JellyCastItem
import dev.jdtech.jellyfin.presentation.theme.JellyCastTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.core.R as CoreR

@Composable
fun PlayButton(
    item: JellyCastItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val runtimeMinutes by remember(item.playbackPositionTicks) {
        mutableLongStateOf(item.playbackPositionTicks.div(600000000))
    }

    Button(
        onClick = onClick,
        modifier = modifier.padding(end = 4.dp),
        enabled = enabled,
    ) {
        Icon(
            painter = painterResource(CoreR.drawable.ic_play),
            contentDescription = null,
        )
        Spacer(modifier = Modifier.width(MaterialTheme.spacings.small))
        Text(
            text = if (runtimeMinutes > 0) {
                stringResource(CoreR.string.runtime_minutes, runtimeMinutes)
            } else {
                stringResource(CoreR.string.play)
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PlayButtonMoviePreview() {
    JellyCastTheme {
        PlayButton(
            item = dummyMovie,
            onClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PlayButtonEpisodePreview() {
    JellyCastTheme {
        PlayButton(
            item = dummyEpisode,
            onClick = {},
        )
    }
}
