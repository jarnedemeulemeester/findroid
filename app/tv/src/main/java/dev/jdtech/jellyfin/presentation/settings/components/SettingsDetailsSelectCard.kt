package dev.jdtech.jellyfin.presentation.settings.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ClickableSurfaceScale
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.RadioButton
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.jdtech.jellyfin.Constants
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.settings.presentation.models.PreferenceSelect
import dev.jdtech.jellyfin.core.R as CoreR

@Composable
fun SettingsDetailsCard(
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

    Surface(
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = MaterialTheme.spacings.default,
                vertical = MaterialTheme.spacings.medium,
            ),
        ) {
            Text(text = stringResource(id = preference.nameStringResource), style = MaterialTheme.typography.headlineMedium)
            preference.descriptionStringRes?.let {
                Spacer(modifier = Modifier.height(MaterialTheme.spacings.small))
                Text(text = stringResource(id = it), style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.height(MaterialTheme.spacings.default))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium - MaterialTheme.spacings.extraSmall),
                contentPadding = PaddingValues(vertical = MaterialTheme.spacings.extraSmall),
            ) {
                items(options) { option ->
                    SettingsSelectDialogItem(
                        option = option,
                        isSelected = option.first == preference.value,
                        onSelect = onUpdate,
                        isEnabled = preference.enabled,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSelectDialogItem(
    option: Pair<String?, String>,
    isSelected: Boolean,
    onSelect: (String?) -> Unit,
    isEnabled: Boolean = true,
) {
    Surface(
        onClick = { onSelect(option.first) },
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(4.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color.Transparent,
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
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(MaterialTheme.spacings.extraSmall),
        ) {
            RadioButton(
                selected = isSelected,
                onClick = null,
                enabled = isEnabled,
            )
            Spacer(modifier = Modifier.width(MaterialTheme.spacings.medium))
            Text(text = option.second, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Preview
@Composable
private fun SettingsDetailCardPreview() {
    FindroidTheme {
        SettingsDetailsCard(
            preference = PreferenceSelect(
                nameStringResource = CoreR.string.settings_preferred_audio_language,
                backendName = Constants.PREF_AUDIO_LANGUAGE,
                options = CoreR.array.languages,
                optionValues = CoreR.array.languages_values,
            ),
            onUpdate = {},
        )
    }
}
