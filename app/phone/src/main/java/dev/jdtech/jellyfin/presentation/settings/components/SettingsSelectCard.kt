package dev.jdtech.jellyfin.presentation.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
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
import dev.jdtech.jellyfin.Constants
import dev.jdtech.jellyfin.models.PreferenceSelect
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.core.R as CoreR

@Composable
fun SettingsSelectCard(
    preference: PreferenceSelect,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDialog by remember {
        mutableStateOf(false)
    }

    OutlinedCard(
        onClick = {
            showDialog = true
        },
        enabled = preference.enabled,
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
                preference.value?.let {
                    Spacer(modifier = Modifier.height(MaterialTheme.spacings.extraSmall))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }

    if (showDialog) {
        SettingsSelectDialog(
            preference = preference,
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
