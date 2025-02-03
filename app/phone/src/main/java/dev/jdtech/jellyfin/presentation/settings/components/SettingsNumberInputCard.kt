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
import androidx.compose.ui.util.fastFilterNotNull
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.settings.presentation.models.Preference
import dev.jdtech.jellyfin.settings.presentation.models.PreferenceIntInput
import dev.jdtech.jellyfin.settings.presentation.models.PreferenceLongInput
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.settings.domain.models.Preference as PreferenceBackend

@Composable
fun SettingsIntInputCard(
    preference: PreferenceIntInput,
    onUpdate: (value: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsNumberInputCard(
        preference = preference,
        text = listOf(preference.prefix, preference.value, preference.suffix).fastFilterNotNull().joinToString(" "),
        modifier = modifier,
    )
}

@Composable
fun SettingsLongInputCard(
    preference: PreferenceLongInput,
    onUpdate: (value: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsNumberInputCard(
        preference = preference,
        text = listOf(preference.prefix, preference.value, preference.suffix).fastFilterNotNull().joinToString(" "),
        modifier = modifier,
    )
}

@Composable
fun SettingsNumberInputCard(
    preference: Preference,
    text: String,
    modifier: Modifier = Modifier,
) {
    var showDialog by remember {
        mutableStateOf(false)
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
                    text = text,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Preview
@Composable
private fun SettingsIntInputCardPreview() {
    FindroidTheme {
        SettingsIntInputCard(
            preference = PreferenceIntInput(
                nameStringResource = CoreR.string.settings_cache_size,
                backendPreference = PreferenceBackend("", 0),
                suffix = "MB",
                value = 25,
            ),
            onUpdate = {},
        )
    }
}

@Preview
@Composable
private fun SettingsLongInputCardPreview() {
    FindroidTheme {
        SettingsLongInputCard(
            preference = PreferenceLongInput(
                nameStringResource = CoreR.string.settings_cache_size,
                backendPreference = PreferenceBackend("", 0L),
                suffix = "MB",
                value = 25,
            ),
            onUpdate = {},
        )
    }
}
