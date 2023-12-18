package dev.jdtech.jellyfin.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
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
import dev.jdtech.jellyfin.utils.ObserveAsEvents
import dev.jdtech.jellyfin.viewmodels.SettingsEvent
import dev.jdtech.jellyfin.viewmodels.SettingsViewModel
import dev.jdtech.jellyfin.core.R as CoreR

@Destination
@Composable
fun SettingsScreen(
    indexes: IntArray = intArrayOf(),
    @StringRes title: Int? = null,
    navigator: DestinationsNavigator,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    LaunchedEffect(true) {
        settingsViewModel.loadPreferences(indexes)
    }

    ObserveAsEvents(settingsViewModel.eventsChannelFlow) { event ->
        when (event) {
            is SettingsEvent.NavigateToSettings -> {
                navigator.navigate(SettingsScreenDestination(event.indexes, event.title))
            }
            is SettingsEvent.NavigateToUsers -> {
                navigator.navigate(UserSelectScreenDestination)
            }
            is SettingsEvent.NavigateToServers -> {
                navigator.navigate(ServerSelectScreenDestination)
            }
        }
    }

    val delegatedUiState by settingsViewModel.uiState.collectAsState()

    SettingsScreenLayout(delegatedUiState, title) { preference ->
        when (preference) {
            is PreferenceSwitch -> {
                settingsViewModel.setBoolean(preference.backendName, preference.value)
            }
            is PreferenceSelect -> {
                settingsViewModel.setString(preference.backendName, preference.value)
            }
        }
        settingsViewModel.loadPreferences(indexes)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SettingsScreenLayout(
    uiState: SettingsViewModel.UiState,
    @StringRes title: Int? = null,
    onUpdate: (Preference) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }

    when (uiState) {
        is SettingsViewModel.UiState.Normal -> {
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
                items(uiState.preferences) { preference ->
                    when (preference) {
                        is PreferenceCategory -> SettingsCategoryCard(preference = preference)
                        is PreferenceSwitch -> {
                            SettingsSwitchCard(preference = preference) {
                                onUpdate(preference.copy(value = !preference.value))
                            }
                        }
                        is PreferenceSelect -> {
                            SettingsSelectCard(preference = preference) {
                                val currentIndex = preference.options.indexOf(preference.value)
                                val newIndex = if (currentIndex == preference.options.count() - 1) {
                                    0
                                } else {
                                    currentIndex + 1
                                }
                                onUpdate(preference.copy(value = preference.options[newIndex]))
                            }
                        }
                    }
                }
            }
            LaunchedEffect(true) {
                focusRequester.requestFocus()
            }
        }
        is SettingsViewModel.UiState.Loading -> {
            Text(text = "LOADING")
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Preview(widthDp = 960, heightDp = 540)
@Composable
private fun SettingsScreenLayoutPreview() {
    FindroidTheme {
        Surface {
            SettingsScreenLayout(
                uiState = SettingsViewModel.UiState.Normal(
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
                ),
                onUpdate = {},
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
                uiState = SettingsViewModel.UiState.Normal(
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
                ),
                title = CoreR.string.settings_category_player,
                onUpdate = {},
            )
        }
    }
}
