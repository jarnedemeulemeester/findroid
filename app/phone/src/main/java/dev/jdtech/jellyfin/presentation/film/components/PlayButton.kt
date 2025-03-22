package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
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
import dev.jdtech.jellyfin.core.presentation.dummy.dummyMovie
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.core.R as CoreR

@Composable
fun PlayButton(
    item: FindroidItem,
) {
    val runtimeMinutes by remember(item.playbackPositionTicks) {
        mutableLongStateOf(item.playbackPositionTicks.div(600000000))
    }
    if (runtimeMinutes > 0) {
        Button(
            onClick = {},
        ) {
            Icon(
                painter = painterResource(CoreR.drawable.ic_play),
                contentDescription = null,
            )
            Spacer(modifier = Modifier.width(MaterialTheme.spacings.small))
            Text(
                text = stringResource(CoreR.string.runtime_minutes, runtimeMinutes),
            )
        }
    } else {
        FilledIconButton(
            onClick = {},
            modifier = Modifier.width(72.dp).padding(end = 4.dp),
        ) {
            Icon(
                painter = painterResource(CoreR.drawable.ic_play),
                contentDescription = null,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PlayButtonPreview() {
    FindroidTheme {
        PlayButton(
            item = dummyMovie,
        )
    }
}
