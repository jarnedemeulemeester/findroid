package dev.jdtech.jellyfin.presentation.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.settings.presentation.models.PreferenceTextInput
import dev.jdtech.jellyfin.settings.R as SettingsR
import dev.jdtech.jellyfin.settings.domain.models.Preference as PreferenceBackend

@Composable
fun SettingsTextInputCard(
    preference: PreferenceTextInput,
    onUpdate: (value: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDialog by remember {
        mutableStateOf(false)
    }

    val displayValue = if (preference.isPassword && preference.value.isNotEmpty()) {
        "••••••••"
    } else if (preference.value.isEmpty()) {
        preference.hintRes?.let { stringResource(it) } ?: ""
    } else {
        preference.value
    }

    SettingsBaseCard(
        preference = preference,
        onClick = {
            showDialog = true
        },
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
                Spacer(modifier = Modifier.height(MaterialTheme.spacings.extraSmall))
                Text(
                    text = displayValue,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (preference.value.isEmpty()) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
            }
        }
    }

    if (showDialog) {
        SettingsTextInputDialog(
            preference = preference,
            onUpdate = { value ->
                showDialog = false
                onUpdate(value)
            },
            onDismissRequest = {
                showDialog = false
            },
        )
    }
}

@Preview
@Composable
private fun SettingsTextInputCardPreview() {
    FindroidTheme {
        SettingsTextInputCard(
            preference = PreferenceTextInput(
                nameStringResource = SettingsR.string.settings_proxy_host,
                hintRes = SettingsR.string.settings_proxy_host_hint,
                backendPreference = PreferenceBackend("", ""),
                value = "proxy.example.com",
            ),
            onUpdate = {},
        )
    }
}

@Preview
@Composable
private fun SettingsTextInputCardPasswordPreview() {
    FindroidTheme {
        SettingsTextInputCard(
            preference = PreferenceTextInput(
                nameStringResource = SettingsR.string.settings_proxy_password,
                backendPreference = PreferenceBackend("", ""),
                isPassword = true,
                value = "secret",
            ),
            onUpdate = {},
        )
    }
}
