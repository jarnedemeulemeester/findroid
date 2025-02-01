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
import dev.jdtech.jellyfin.Constants
import dev.jdtech.jellyfin.models.Preference
import dev.jdtech.jellyfin.models.PreferenceCategory
import dev.jdtech.jellyfin.models.PreferenceGroup
import dev.jdtech.jellyfin.models.PreferenceSelect
import dev.jdtech.jellyfin.models.PreferenceSwitch
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.settings.presentation.settings.SettingsAction
import dev.jdtech.jellyfin.core.R as CoreR

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
                style = MaterialTheme.typography.titleSmall
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
                            color = DividerDefaults.color.copy(alpha = 0.2f)
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
                nameStringResource = CoreR.string.mpv_player,
                preferences = listOf(
                    PreferenceSwitch(
                        nameStringResource = CoreR.string.mpv_player,
                        descriptionStringRes = CoreR.string.mpv_player_summary,
                        backendName = Constants.PREF_PLAYER_MPV,
                        backendDefaultValue = false,
                    ),
                    PreferenceSelect(
                        nameStringResource = CoreR.string.pref_player_mpv_hwdec,
                        dependencies = listOf(Constants.PREF_PLAYER_MPV),
                        backendName = Constants.PREF_PLAYER_MPV_HWDEC,
                        backendDefaultValue = "mediacodec",
                        options = CoreR.array.mpv_hwdec,
                        optionValues = CoreR.array.mpv_hwdec,
                    ),
                    PreferenceSelect(
                        nameStringResource = CoreR.string.pref_player_mpv_vo,
                        dependencies = listOf(Constants.PREF_PLAYER_MPV),
                        backendName = Constants.PREF_PLAYER_MPV_VO,
                        backendDefaultValue = "gpu-next",
                        options = CoreR.array.mpv_vos,
                        optionValues = CoreR.array.mpv_vos,
                    ),
                    PreferenceSelect(
                        nameStringResource = CoreR.string.pref_player_mpv_ao,
                        dependencies = listOf(Constants.PREF_PLAYER_MPV),
                        backendName = Constants.PREF_PLAYER_MPV_AO,
                        backendDefaultValue = "audiotrack",
                        options = CoreR.array.mpv_aos,
                        optionValues = CoreR.array.mpv_aos,
                    ),
                ),
            ),
            onAction = {},
        )
    }
}
