package dev.jdtech.jellyfin.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ClickableSurfaceScale
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.jdtech.jellyfin.models.PreferenceSelect
import dev.jdtech.jellyfin.ui.theme.FindroidTheme
import dev.jdtech.jellyfin.ui.theme.spacings
import dev.jdtech.jellyfin.viewmodels.SettingsViewModel
import dev.jdtech.jellyfin.core.R as CoreR

@Composable
fun SettingsSelectCard(
    preference: PreferenceSelect,
    settingViewModel: SettingsViewModel = hiltViewModel(),
) {
    val initialValue = settingViewModel.getString(preference.backendName, preference.backendDefaultValue)

    SettingsSelectCardLayout(preference = preference, initialValue = initialValue) { newValue ->
        preference.backendName.let { key ->
            settingViewModel.setString(key, newValue)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SettingsSelectCardLayout(
    preference: PreferenceSelect,
    initialValue: String?,
    onUpdate: (String?) -> Unit,
) {
    var value by remember {
        mutableStateOf(initialValue)
    }

    Surface(
        onClick = {
            onUpdate(value)
            preference.onClick(preference)
        },
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
            if (preference.iconDrawableId != null) {
                Icon(
                    painter = painterResource(id = preference.iconDrawableId!!),
                    contentDescription = null,
                )
            } else {
                Spacer(modifier = Modifier.size(24.dp))
            }

            Spacer(modifier = Modifier.width(24.dp))
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = stringResource(id = preference.nameStringResource),
                    style = MaterialTheme.typography.titleMedium,
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacings.extraSmall))
                Text(
                    text = value ?: "Not set",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Preview
@Composable
private fun SettingsSelectCardPreview() {
    FindroidTheme {
        Surface {
            SettingsSelectCardLayout(
                preference = PreferenceSelect(
                    nameStringResource = CoreR.string.settings_preferred_audio_language,
                    iconDrawableId = CoreR.drawable.ic_speaker,
                    backendName = "image-cache",
                    backendDefaultValue = null,
                ),
                initialValue = null,
                onUpdate = {},
            )
        }
    }
}
