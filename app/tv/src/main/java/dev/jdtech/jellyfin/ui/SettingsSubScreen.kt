package dev.jdtech.jellyfin.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.MaterialTheme
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
import dev.jdtech.jellyfin.ui.components.SettingsDetailsCard
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
fun SettingsSubScreen(
    indexes: IntArray = intArrayOf(),
    @StringRes title: Int,
    navigator: DestinationsNavigator,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    LaunchedEffect(true) {
        settingsViewModel.loadPreferences(indexes)
    }

    ObserveAsEvents(settingsViewModel.eventsChannelFlow) { event ->
        when (event) {
            is SettingsEvent.NavigateToSettings -> {
                navigator.navigate(SettingsScreenDestination)
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

    SettingsSubScreenLayout(delegatedUiState, title) { preference ->
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

@Composable
private fun SettingsSubScreenLayout(
    uiState: SettingsViewModel.UiState,
    @StringRes title: Int? = null,
    onUpdate: (Preference) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }

    when (uiState) {
        is SettingsViewModel.UiState.Normal -> {
            var focusedPreference by remember {
                mutableStateOf(uiState.preferences.first())
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = MaterialTheme.spacings.large,
                        top = MaterialTheme.spacings.default * 2,
                        end = MaterialTheme.spacings.large,
                    ),
            ) {
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.large),
                ) {
                    TvLazyColumn(
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.default),
                        contentPadding = PaddingValues(vertical = MaterialTheme.spacings.large),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                    ) {
                        items(uiState.preferences) { preference ->
                            when (preference) {
                                is PreferenceCategory -> SettingsCategoryCard(
                                    preference = preference,
                                    modifier = Modifier.onFocusChanged {
                                        if (it.isFocused) {
                                            focusedPreference = preference
                                        }
                                    },
                                )
                                is PreferenceSwitch -> {
                                    SettingsSwitchCard(
                                        preference = preference,
                                        modifier = Modifier.onFocusChanged {
                                            if (it.isFocused) {
                                                focusedPreference = preference
                                            }
                                        },
                                    ) {
                                        onUpdate(preference.copy(value = !preference.value))
                                    }
                                }
                                is PreferenceSelect -> {
                                    val optionValues = stringArrayResource(id = preference.optionValues)
                                    SettingsSelectCard(
                                        preference = preference,
                                        modifier = Modifier.onFocusChanged {
                                            if (it.isFocused) {
                                                focusedPreference = preference
                                            }
                                        },
                                    ) {
                                        val currentIndex = optionValues.indexOf(preference.value)
                                        val newIndex = if (currentIndex == optionValues.count() - 1) {
                                            0
                                        } else {
                                            currentIndex + 1
                                        }
                                        val newPreference = preference.copy(value = optionValues[newIndex])
                                        onUpdate(newPreference)
                                        if (focusedPreference == preference) {
                                            focusedPreference = newPreference
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Box(
                        modifier = Modifier.weight(2f),
                    ) {
                        (focusedPreference as? PreferenceSelect)?.let {
                            SettingsDetailsCard(
                                preference = it,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(bottom = MaterialTheme.spacings.large),
                                onOptionSelected = { value ->
                                    println(value)
                                    val newPreference = it.copy(value = value)
                                    onUpdate(newPreference)
                                    focusedPreference = newPreference
                                },
                            )
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

@Preview(device = "id:tv_1080p")
@Composable
private fun SettingsSubScreenLayoutPreview() {
    FindroidTheme {
        SettingsSubScreenLayout(
            uiState = SettingsViewModel.UiState.Normal(
                listOf(
                    PreferenceSelect(
                        nameStringResource = CoreR.string.pref_player_mpv_hwdec,
                        backendName = Constants.PREF_PLAYER_MPV_HWDEC,
                        backendDefaultValue = "mediacodec",
                        options = CoreR.array.mpv_hwdec,
                        optionValues = CoreR.array.mpv_hwdec,
                    ),
                ),
            ),
            title = CoreR.string.settings_category_player,
            onUpdate = {},
        )
    }
}
