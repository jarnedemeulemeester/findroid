package dev.jdtech.jellyfin.presentation.settings.components

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ClickableSurfaceScale
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.jdtech.jellyfin.Constants
import dev.jdtech.jellyfin.models.PreferenceSelect
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.core.R as CoreR

@Composable
fun SettingsSelectCard(
    preference: PreferenceSelect,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
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

    Surface(
        onClick = onClick,
        enabled = preference.enabled,
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(10.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            disabledContainerColor = MaterialTheme.colorScheme.surface,
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
                    text = optionsMap.getOrDefault(preference.value, notSetString),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Preview
@Composable
private fun SettingsSelectCardPreview() {
    FindroidTheme {
        SettingsSelectCard(
            preference = PreferenceSelect(
                nameStringResource = CoreR.string.settings_preferred_audio_language,
                iconDrawableId = CoreR.drawable.ic_speaker,
                backendName = Constants.PREF_AUDIO_LANGUAGE,
                backendDefaultValue = null,
                options = CoreR.array.languages,
                optionValues = CoreR.array.languages_values,
            ),
            onClick = {},
        )
    }
}
