package dev.jdtech.jellyfin.presentation.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import dev.jdtech.jellyfin.models.PreferenceSwitch
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.core.R as CoreR

@Composable
fun SettingsSwitchCard(
    preference: PreferenceSwitch,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsBaseCard(
        preference = preference,
        onClick = onClick,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(MaterialTheme.spacings.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (preference.iconDrawableId != null) {
                Icon(
                    painter = painterResource(preference.iconDrawableId!!),
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(MaterialTheme.spacings.default))
            }
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = stringResource(preference.nameStringResource),
                    style = MaterialTheme.typography.titleMedium,
                )
                preference.descriptionStringRes?.let {
                    Spacer(modifier = Modifier.height(MaterialTheme.spacings.extraSmall))
                    Text(
                        text = stringResource(id = it),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            Spacer(modifier = Modifier.width(MaterialTheme.spacings.default))
            Switch(
                checked = preference.enabled && preference.value,
                onCheckedChange = {
                    onClick()
                },
                enabled = preference.enabled,
            )
        }
    }
}

@Preview
@Composable
private fun SettingsSwitchCardPreview() {
    FindroidTheme {
        SettingsSwitchCard(
            preference = PreferenceSwitch(
                nameStringResource = CoreR.string.settings_use_cache_title,
                backendName = "image-cache",
                backendDefaultValue = false,
                value = false,
            ),
            onClick = {},
        )
    }
}

@Preview
@Composable
private fun SettingsSwitchCardDisabledPreview() {
    FindroidTheme {
        SettingsSwitchCard(
            preference = PreferenceSwitch(
                nameStringResource = CoreR.string.settings_use_cache_title,
                enabled = false,
                backendName = "image-cache",
                backendDefaultValue = false,
            ),
            onClick = {},
        )
    }
}
