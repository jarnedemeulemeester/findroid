package dev.jdtech.jellyfin.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.tv.foundation.lazy.grid.TvGridCells
import androidx.tv.foundation.lazy.grid.TvGridItemSpan
import androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid
import androidx.tv.foundation.lazy.grid.items
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.jdtech.jellyfin.Constants
import dev.jdtech.jellyfin.destinations.ServerSelectScreenDestination
import dev.jdtech.jellyfin.destinations.SettingsScreenDestination
import dev.jdtech.jellyfin.destinations.UserSelectScreenDestination
import dev.jdtech.jellyfin.models.Preference
import dev.jdtech.jellyfin.models.PreferenceCategory
import dev.jdtech.jellyfin.models.PreferenceSelect
import dev.jdtech.jellyfin.models.PreferenceSwitch
import dev.jdtech.jellyfin.ui.components.SettingsCategoryCard
import dev.jdtech.jellyfin.ui.components.SettingsSelectCard
import dev.jdtech.jellyfin.ui.components.SettingsSwitchCard
import dev.jdtech.jellyfin.ui.theme.FindroidTheme
import dev.jdtech.jellyfin.ui.theme.spacings
import dev.jdtech.jellyfin.core.R as CoreR

@Destination
@Composable
fun SettingsScreen(
    indexes: IntArray = intArrayOf(),
    @StringRes title: Int? = null,
    navigator: DestinationsNavigator,
) {
    val topLevelPreferences = listOf<Preference>(
        PreferenceCategory(
            nameStringResource = CoreR.string.settings_category_language,
            iconDrawableId = CoreR.drawable.ic_languages,
            onClick = {
                navigator.navigate(SettingsScreenDestination(intArrayOf(0), it.nameStringResource))
            },
            nestedPreferences = listOf(
                PreferenceSelect(
                    nameStringResource = CoreR.string.settings_preferred_audio_language,
                    iconDrawableId = CoreR.drawable.ic_speaker,
                    backendName = Constants.PREF_AUDIO_LANGUAGE,
                    backendDefaultValue = null,
                ),
                PreferenceSelect(
                    nameStringResource = CoreR.string.settings_preferred_subtitle_language,
                    iconDrawableId = CoreR.drawable.ic_closed_caption,
                    backendName = Constants.PREF_SUBTITLE_LANGUAGE,
                    backendDefaultValue = null,
                ),
            ),
        ),
        PreferenceCategory(
            nameStringResource = CoreR.string.settings_category_appearance,
            iconDrawableId = CoreR.drawable.ic_palette,
        ),
        PreferenceCategory(
            nameStringResource = CoreR.string.settings_category_player,
            iconDrawableId = CoreR.drawable.ic_play,
        ),
        PreferenceCategory(
            nameStringResource = CoreR.string.users,
            iconDrawableId = CoreR.drawable.ic_user,
            onClick = {
                navigator.navigate(UserSelectScreenDestination)
            },
        ),
        PreferenceCategory(
            nameStringResource = CoreR.string.settings_category_servers,
            iconDrawableId = CoreR.drawable.ic_server,
            onClick = {
                navigator.navigate(ServerSelectScreenDestination)
            },
        ),
        PreferenceCategory(
            nameStringResource = CoreR.string.settings_category_device,
            iconDrawableId = CoreR.drawable.ic_smartphone,
        ),
        PreferenceCategory(
            nameStringResource = CoreR.string.settings_category_network,
            iconDrawableId = CoreR.drawable.ic_network,
        ),
        PreferenceCategory(
            nameStringResource = CoreR.string.settings_category_cache,
            iconDrawableId = CoreR.drawable.ic_hard_drive,
            onClick = {
                navigator.navigate(SettingsScreenDestination(intArrayOf(7), it.nameStringResource))
            },
            nestedPreferences = listOf(
                PreferenceSwitch(
                    nameStringResource = CoreR.string.settings_use_cache_title,
                    descriptionStringRes = CoreR.string.settings_use_cache_summary,
                    backendName = Constants.PREF_IMAGE_CACHE,
                    backendDefaultValue = false,
                    onClick = {
                    },
                ),
            ),
        ),
        PreferenceCategory(
            nameStringResource = CoreR.string.about,
            iconDrawableId = CoreR.drawable.ic_info,
        ),
    )

    var preferences = topLevelPreferences

    for (index in indexes) {
        val preference = preferences[index]
        if (preference is PreferenceCategory) {
            preferences = preference.nestedPreferences
        }
    }

    SettingsScreenLayout(preferences, title)
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SettingsScreenLayout(
    preferences: List<Preference>,
    @StringRes title: Int? = null,
) {
    val focusRequester = remember { FocusRequester() }

    TvLazyVerticalGrid(
        columns = TvGridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.default),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.default),
        contentPadding = PaddingValues(horizontal = MaterialTheme.spacings.default * 2, vertical = MaterialTheme.spacings.large),
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(Color.Black, Color(0xFF001721))))
            .focusRequester(focusRequester),
    ) {
        item(span = { TvGridItemSpan(this.maxLineSpan) }) {
            if (title != null) {
                Column {
                    Text(
                        text = stringResource(id = title),
                        style = MaterialTheme.typography.displayMedium,
                    )
                    Text(
                        text = stringResource(id = CoreR.string.title_settings),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                }
            } else {
                Text(
                    text = stringResource(id = CoreR.string.title_settings),
                    style = MaterialTheme.typography.displayMedium,
                )
            }
        }
        items(preferences) { preference ->
            when (preference) {
                is PreferenceCategory -> SettingsCategoryCard(preference = preference)
                is PreferenceSwitch -> SettingsSwitchCard(preference = preference)
                is PreferenceSelect -> SettingsSelectCard(preference = preference)
            }
        }
    }
    LaunchedEffect(true) {
        focusRequester.requestFocus()
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Preview(widthDp = 960, heightDp = 540)
@Composable
private fun SettingsScreenLayoutPreview() {
    FindroidTheme {
        Surface {
            SettingsScreenLayout(
                listOf(
                    PreferenceCategory(
                        nameStringResource = CoreR.string.settings_category_language,
                        iconDrawableId = CoreR.drawable.ic_languages,
                    ),
                    PreferenceCategory(
                        nameStringResource = CoreR.string.settings_category_appearance,
                        iconDrawableId = CoreR.drawable.ic_palette,
                    ),
                ),
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Preview(widthDp = 960, heightDp = 540)
@Composable
private fun SettingsScreenLayoutNestedPreview() {
    FindroidTheme {
        Surface {
            SettingsScreenLayout(
                preferences = listOf(
                    PreferenceCategory(
                        nameStringResource = CoreR.string.settings_category_language,
                        iconDrawableId = CoreR.drawable.ic_languages,
                    ),
                    PreferenceCategory(
                        nameStringResource = CoreR.string.settings_category_appearance,
                        iconDrawableId = CoreR.drawable.ic_palette,
                    ),
                ),
                title = CoreR.string.settings_category_player,
            )
        }
    }
}
