package dev.jdtech.jellyfin.presentation.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.util.fastForEachIndexed
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.settings.presentation.models.Preference
import dev.jdtech.jellyfin.settings.presentation.models.PreferenceCategory
import dev.jdtech.jellyfin.settings.presentation.models.PreferenceGroup
import dev.jdtech.jellyfin.settings.presentation.models.PreferenceSelect
import dev.jdtech.jellyfin.settings.presentation.models.PreferenceSwitch
import dev.jdtech.jellyfin.settings.presentation.settings.SettingsAction
import dev.jdtech.jellyfin.settings.R as SettingsR
import dev.jdtech.jellyfin.settings.domain.models.Preference as PreferenceBackend

@Composable
fun SettingsGroupCard(
    group: PreferenceGroup,
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier,
    onFocusChange: (FocusState, Preference) -> Unit = { _, _ -> },
) {
    Column {
        group.nameStringResource?.let {
            Text(
                text = stringResource(it),
                modifier = Modifier.padding(start = MaterialTheme.spacings.medium),
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(modifier.height(MaterialTheme.spacings.small))
        }
        Surface(
            shape = MaterialTheme.shapes.medium,
            colors = SurfaceDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            modifier = modifier
                .fillMaxWidth(),
        ) {
            Column {
                group.preferences.fastForEachIndexed { index, preference ->
                    when (preference) {
                        is PreferenceCategory -> SettingsCategoryCard(
                            preference = preference,
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged {
                                    onFocusChange(it, preference)
                                },
                        )
                        is PreferenceSwitch -> SettingsSwitchCard(
                            preference = preference,
                            onClick = {
                                onAction(
                                    SettingsAction.OnUpdate(
                                        preference.copy(value = !preference.value),
                                    ),
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged {
                                    onFocusChange(it, preference)
                                },
                        )
                        is PreferenceSelect -> SettingsSelectCard(
                            preference = preference,
                            onClick = { },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged {
                                    onFocusChange(it, preference)
                                },
                        )
                    }
                    if (index < group.preferences.lastIndex) {
                        HorizontalDivider(
                            color = DividerDefaults.color.copy(alpha = 0.2f),
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun SettingsGroupCardPreview() {
    FindroidTheme {
        SettingsGroupCard(
            group = PreferenceGroup(
                nameStringResource = SettingsR.string.mpv_player,
                preferences = listOf(
                    PreferenceSwitch(
                        nameStringResource = SettingsR.string.mpv_player,
                        descriptionStringRes = SettingsR.string.mpv_player_summary,
                        backendPreference = PreferenceBackend("", false),
                    ),
                    PreferenceSelect(
                        nameStringResource = SettingsR.string.pref_player_mpv_hwdec,
                        dependencies = listOf(PreferenceBackend("", false)),
                        backendPreference = PreferenceBackend("", ""),
                        options = SettingsR.array.mpv_hwdec,
                        optionValues = SettingsR.array.mpv_hwdec,
                    ),
                    PreferenceSelect(
                        nameStringResource = SettingsR.string.pref_player_mpv_vo,
                        dependencies = listOf(PreferenceBackend("", false)),
                        backendPreference = PreferenceBackend("", ""),
                        options = SettingsR.array.mpv_vos,
                        optionValues = SettingsR.array.mpv_vos,
                    ),
                    PreferenceSelect(
                        nameStringResource = SettingsR.string.pref_player_mpv_ao,
                        dependencies = listOf(PreferenceBackend("", false)),
                        backendPreference = PreferenceBackend("", ""),
                        options = SettingsR.array.mpv_aos,
                        optionValues = SettingsR.array.mpv_aos,
                    ),
                ),
            ),
            onAction = {},
        )
    }
}
