package dev.jdtech.jellyfin.presentation.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import dev.jdtech.jellyfin.Constants
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
        OutlinedCard(
            modifier = modifier,
        ) {
            group.preferences.forEach { preference ->
                when (preference) {
                    is PreferenceCategory -> SettingsCategoryCard(
                        preference = preference,
                        modifier = Modifier
                            .fillMaxWidth(),
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
                            .fillMaxWidth(),
                    )
                    is PreferenceSelect -> SettingsSelectCard(
                        preference = preference,
                        onUpdate = { value ->
                            onAction(
                                SettingsAction.OnUpdate(
                                    preference.copy(value = value),
                                ),
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth(),
                    )
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
