package dev.jdtech.jellyfin.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.media3.common.C.DEFAULT_SEEK_BACK_INCREMENT_MS
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ClickableSurfaceScale
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.jdtech.jellyfin.Constants
import dev.jdtech.jellyfin.models.PreferenceLong
import dev.jdtech.jellyfin.ui.theme.FindroidTheme
import dev.jdtech.jellyfin.ui.theme.spacings
import dev.jdtech.jellyfin.core.R as CoreR

@Composable
fun SettingsLongCard(
    preference: PreferenceLong,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = preference.enabled,
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
        modifier = modifier
            .fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(MaterialTheme.spacings.default),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (preference.iconDrawableId != null) {
                Icon(
                    painter = painterResource(id = preference.iconDrawableId!!),
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(24.dp))
            }

            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = stringResource(id = preference.nameStringResource),
                    style = MaterialTheme.typography.titleMedium,
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacings.extraSmall))
                Text(
                    text = preference.value.toString(),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Preview
@Composable
private fun SettingsLongCardPreview() {
    FindroidTheme {
        SettingsLongCard(
            preference = PreferenceLong(
                nameStringResource = CoreR.string.seek_back_increment,
                backendName = Constants.PREF_PLAYER_SEEK_BACK_INC,
                backendDefaultValue = DEFAULT_SEEK_BACK_INCREMENT_MS,
                value = DEFAULT_SEEK_BACK_INCREMENT_MS,
            ),
            onClick = {},
        )
    }
}
