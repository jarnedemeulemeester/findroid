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
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.settings.R as SettingsR
import dev.jdtech.jellyfin.settings.domain.models.Preference
import dev.jdtech.jellyfin.settings.presentation.models.PreferenceDynamicSelect
import dev.jdtech.jellyfin.settings.presentation.models.PreferenceSelect

@Composable
fun SettingsDynamicSelectCard(
    preference: PreferenceDynamicSelect,
    onUpdate: (value: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val notSetString = stringResource(CoreR.string.not_set)
    val optionsMap = remember(preference.dynamicOptions) { preference.dynamicOptions.toMap() }
    // preference.value == null means "not yet set" → treat as "0" (internal storage)
    val displayValue = preference.value ?: "0"
    var showDialog by remember { mutableStateOf(false) }

    SettingsBaseCard(
        preference = preference,
        onClick = { showDialog = true },
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(preference.nameStringResource),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacings.extraSmall))
                Text(
                    text = optionsMap.getOrDefault(displayValue, notSetString),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }

    if (showDialog) {
        SettingsSelectDialog(
            // SettingsSelectDialog accepts a PreferenceSelect; we build a synthetic one
            // using the existing SettingsSelectDialog overload that takes options directly.
            preference = PreferenceSelect(
                nameStringResource = preference.nameStringResource,
                backendPreference = preference.backendPreference,
                options = 0,        // unused — dialog receives the list directly
                optionValues = 0,   // unused
                // Normalize null to "0" so the internal-storage radio button is
                // highlighted when the preference has never been explicitly set.
                value = preference.value ?: "0",
            ),
            options = preference.dynamicOptions,
            onUpdate = { value ->
                showDialog = false
                onUpdate(value)
            },
            onDismissRequest = { showDialog = false },
        )
    }
}

@Preview
@Composable
private fun SettingsDynamicSelectCardPreview() {
    FindroidTheme {
        SettingsDynamicSelectCard(
            preference = PreferenceDynamicSelect(
                nameStringResource = SettingsR.string.pref_download_storage_location,
                backendPreference = Preference("", null),
                dynamicOptions = listOf("0" to "Internal storage (42 GB free)", "1" to "SD card 1 (8 GB free)"),
                value = "0",
            ),
            onUpdate = {},
        )
    }
}
