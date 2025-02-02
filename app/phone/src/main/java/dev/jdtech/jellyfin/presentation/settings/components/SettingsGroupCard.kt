package dev.jdtech.jellyfin.presentation.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.util.fastForEachIndexed
import dev.jdtech.jellyfin.Constants
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.settings.presentation.models.PreferenceCategory
import dev.jdtech.jellyfin.settings.presentation.models.PreferenceGroup
import dev.jdtech.jellyfin.settings.presentation.models.PreferenceNumberInput
import dev.jdtech.jellyfin.settings.presentation.models.PreferenceSelect
import dev.jdtech.jellyfin.settings.presentation.models.PreferenceSwitch
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
        Card(
            modifier = modifier,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
        ) {
            group.preferences.fastForEachIndexed { index, preference ->
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
                    is PreferenceNumberInput -> SettingsNumberInputCard(
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
                if (index < group.preferences.lastIndex) {
                    HorizontalDivider(
                        color = DividerDefaults.color.copy(alpha = 0.2f),
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
                        backendName = "",
                    ),
                    PreferenceSelect(
                        nameStringResource = CoreR.string.pref_player_mpv_hwdec,
                        dependencies = listOf(Constants.PREF_PLAYER_MPV),
                        backendName = Constants.PREF_PLAYER_MPV_HWDEC,
                        options = CoreR.array.mpv_hwdec,
                        optionValues = CoreR.array.mpv_hwdec,
                    ),
                    PreferenceSelect(
                        nameStringResource = CoreR.string.pref_player_mpv_vo,
                        dependencies = listOf(Constants.PREF_PLAYER_MPV),
                        backendName = Constants.PREF_PLAYER_MPV_VO,
                        options = CoreR.array.mpv_vos,
                        optionValues = CoreR.array.mpv_vos,
                    ),
                    PreferenceSelect(
                        nameStringResource = CoreR.string.pref_player_mpv_ao,
                        dependencies = listOf(Constants.PREF_PLAYER_MPV),
                        backendName = Constants.PREF_PLAYER_MPV_AO,
                        options = CoreR.array.mpv_aos,
                        optionValues = CoreR.array.mpv_aos,
                    ),
                ),
            ),
            onAction = {},
        )
    }
}
