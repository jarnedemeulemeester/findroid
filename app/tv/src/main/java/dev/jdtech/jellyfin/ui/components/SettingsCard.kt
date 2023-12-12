package dev.jdtech.jellyfin.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ClickableSurfaceScale
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.jdtech.jellyfin.ui.theme.FindroidTheme
import dev.jdtech.jellyfin.ui.theme.spacings
import dev.jdtech.jellyfin.core.R as CoreR

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SettingsCard(
    name: String,
    @DrawableRes icon: Int,
    onClick: (String) -> Unit,
) {
    Surface(
        onClick = { onClick(name) },
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(10.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                BorderStroke(
                    4.dp,
                    Color.White,
                ),
                shape = RoundedCornerShape(10.dp),
            ),
        ),
        scale = ClickableSurfaceScale.None,
        modifier = Modifier
            .fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(MaterialTheme.spacings.default),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
            )
            Spacer(modifier = Modifier.width(24.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Preview
@Composable
private fun SettingsCardPreview() {
    FindroidTheme {
        Surface {
            SettingsCard(
                name = "Player",
                icon = CoreR.drawable.ic_play,
                onClick = {},
            )
        }
    }
}
