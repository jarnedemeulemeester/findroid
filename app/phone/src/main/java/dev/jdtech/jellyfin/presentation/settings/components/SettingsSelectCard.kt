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
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.settings.domain.models.Preference
import dev.jdtech.jellyfin.settings.presentation.models.PreferenceSelect
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.settings.R as SettingsR

@Composable
fun SettingsSelectCard(
    preference: PreferenceSelect,
    onUpdate: (value: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val optionValues = stringArrayResource(preference.optionValues)
    val optionNames = stringArrayResource(preference.options)
    val notSetString = stringResource(CoreR.string.not_set)

    val options = remember(preference.nameStringResource) {
        val options = mutableListOf<Pair<String?, String>>()

        if (preference.optionsIncludeNull) {
            options.add(Pair(null, notSetString))
        }
        options.addAll(optionValues.zip(optionNames))

        options
    }

    val optionsMap = remember(options) {
        options.toMap()
    }

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
                    text = optionsMap.getOrDefault(preference.value, notSetString),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }

    if (showDialog) {
        SettingsSelectDialog(
            preference = preference,
            options = options,
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
private fun SettingsSelectCardPreview() {
    FindroidTheme {
        SettingsSelectCard(
            preference = PreferenceSelect(
                nameStringResource = SettingsR.string.settings_preferred_audio_language,
                iconDrawableId = CoreR.drawable.ic_speaker,
                backendPreference = Preference("", ""),
                options = SettingsR.array.languages,
                optionValues = SettingsR.array.languages_values,
            ),
            onUpdate = {},
        )
    }
}
