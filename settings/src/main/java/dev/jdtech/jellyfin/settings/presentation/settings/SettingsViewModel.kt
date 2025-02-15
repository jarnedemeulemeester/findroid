package dev.jdtech.jellyfin.settings.presentation.settings

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.settings.R
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import dev.jdtech.jellyfin.settings.presentation.models.PreferenceAppLanguage
import dev.jdtech.jellyfin.settings.presentation.models.PreferenceCategory
import dev.jdtech.jellyfin.settings.presentation.models.PreferenceGroup
import dev.jdtech.jellyfin.settings.presentation.models.PreferenceIntInput
import dev.jdtech.jellyfin.settings.presentation.models.PreferenceLongInput
import dev.jdtech.jellyfin.settings.presentation.models.PreferenceSelect
import dev.jdtech.jellyfin.settings.presentation.models.PreferenceSwitch
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
@Inject
constructor(
    private val appPreferences: AppPreferences,
) : ViewModel() {
    private val _state = MutableStateFlow(SettingsState())
    val state = _state.asStateFlow()

    private val eventsChannel = Channel<SettingsEvent>()
    val events = eventsChannel.receiveAsFlow()

    private val topLevelPreferences = listOf<PreferenceGroup>(
        PreferenceGroup(
            preferences = listOf(
                PreferenceSwitch(
                    nameStringResource = R.string.offline_mode,
                    descriptionStringRes = R.string.offline_mode_summary,
                    iconDrawableId = R.drawable.ic_server_off,
                    backendPreference = appPreferences.offlineMode,
                ),
            ),
        ),
        PreferenceGroup(
            preferences = listOf(
                PreferenceCategory(
                    nameStringResource = R.string.settings_category_language,
                    iconDrawableId = R.drawable.ic_languages,
                    onClick = {
                        viewModelScope.launch {
                            eventsChannel.send(SettingsEvent.NavigateToSettings(intArrayOf(it.nameStringResource)))
                        }
                    },
                    nestedPreferenceGroups = listOf(
                        PreferenceGroup(
                            preferences = listOf(
                                PreferenceAppLanguage(
                                    nameStringResource = R.string.app_language,
                                    iconDrawableId = R.drawable.ic_languages,
                                    enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
                                ),
                            ),
                        ),
                        PreferenceGroup(
                            preferences = listOf(
                                PreferenceSelect(
                                    nameStringResource = R.string.settings_preferred_audio_language,
                                    iconDrawableId = R.drawable.ic_speaker,
                                    backendPreference = appPreferences.preferredAudioLanguage,
                                    options = R.array.languages,
                                    optionValues = R.array.languages_values,
                                    optionsIncludeNull = true,
                                ),
                                PreferenceSelect(
                                    nameStringResource = R.string.settings_preferred_subtitle_language,
                                    iconDrawableId = R.drawable.ic_closed_caption,
                                    backendPreference = appPreferences.preferredSubtitleLanguage,
                                    options = R.array.languages,
                                    optionValues = R.array.languages_values,
                                    optionsIncludeNull = true,
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        ),
        PreferenceGroup(
            preferences = listOf(
                PreferenceCategory(
                    nameStringResource = R.string.settings_category_appearance,
                    iconDrawableId = R.drawable.ic_palette,
                    onClick = {
                        viewModelScope.launch {
                            eventsChannel.send(SettingsEvent.NavigateToSettings(intArrayOf(it.nameStringResource)))
                        }
                    },
                    nestedPreferenceGroups = listOf(
                        PreferenceGroup(
                            preferences = listOf(
                                PreferenceSelect(
                                    nameStringResource = R.string.theme,
                                    backendPreference = appPreferences.theme,
                                    onUpdate = { value ->
                                        viewModelScope.launch {
                                            eventsChannel.send(SettingsEvent.UpdateTheme(value ?: "system"))
                                        }
                                    },
                                    options = R.array.theme,
                                    optionValues = R.array.theme_values,
                                ),
                                PreferenceSwitch(
                                    nameStringResource = R.string.dynamic_colors,
                                    descriptionStringRes = R.string.dynamic_colors_summary,
                                    enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                                    backendPreference = appPreferences.dynamicColors,
                                ),
                            ),
                        ),
                        PreferenceGroup(
                            preferences = listOf(
                                PreferenceSwitch(
                                    nameStringResource = R.string.extra_info,
                                    descriptionStringRes = R.string.extra_info_summary,
                                    backendPreference = appPreferences.displayExtraInfo,
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        ),
        PreferenceGroup(
            preferences = listOf(
                PreferenceCategory(
                    nameStringResource = R.string.settings_category_player,
                    iconDrawableId = R.drawable.ic_play,
                    onClick = {
                        viewModelScope.launch {
                            eventsChannel.send(SettingsEvent.NavigateToSettings(intArrayOf(it.nameStringResource)))
                        }
                    },
                    nestedPreferenceGroups = listOf(
                        PreferenceGroup(
                            nameStringResource = R.string.mpv_player,
                            preferences = listOf(
                                PreferenceSwitch(
                                    nameStringResource = R.string.mpv_player,
                                    descriptionStringRes = R.string.mpv_player_summary,
                                    backendPreference = appPreferences.playerMpv,
                                ),
                                PreferenceSelect(
                                    nameStringResource = R.string.pref_player_mpv_hwdec,
                                    dependencies = listOf(appPreferences.playerMpv),
                                    backendPreference = appPreferences.playerMpvHwdec,
                                    options = R.array.mpv_hwdec,
                                    optionValues = R.array.mpv_hwdec,
                                ),
                                PreferenceSelect(
                                    nameStringResource = R.string.pref_player_mpv_vo,
                                    dependencies = listOf(appPreferences.playerMpv),
                                    backendPreference = appPreferences.playerMpvVo,
                                    options = R.array.mpv_vos,
                                    optionValues = R.array.mpv_vos,
                                ),
                                PreferenceSelect(
                                    nameStringResource = R.string.pref_player_mpv_ao,
                                    dependencies = listOf(appPreferences.playerMpv),
                                    backendPreference = appPreferences.playerMpvAo,
                                    options = R.array.mpv_aos,
                                    optionValues = R.array.mpv_aos,
                                ),
                            ),
                        ),
                        PreferenceGroup(
                            nameStringResource = R.string.gestures,
                            preferences = listOf(
                                PreferenceSwitch(
                                    nameStringResource = R.string.player_gestures,
                                    backendPreference = appPreferences.playerGestures,
                                ),
                                PreferenceSwitch(
                                    nameStringResource = R.string.player_gestures_vb,
                                    descriptionStringRes = R.string.player_gestures_vb_summary,
                                    dependencies = listOf(appPreferences.playerGestures),
                                    backendPreference = appPreferences.playerGesturesVB,
                                ),
                                PreferenceSwitch(
                                    nameStringResource = R.string.player_gestures_seek,
                                    descriptionStringRes = R.string.player_gestures_seek_summary,
                                    dependencies = listOf(appPreferences.playerGestures),
                                    backendPreference = appPreferences.playerGesturesSeek,
                                ),
                                PreferenceSwitch(
                                    nameStringResource = R.string.player_gestures_zoom,
                                    descriptionStringRes = R.string.player_gestures_zoom_summary,
                                    dependencies = listOf(appPreferences.playerGestures),
                                    backendPreference = appPreferences.playerGesturesZoom,
                                ),
                                PreferenceSwitch(
                                    nameStringResource = R.string.player_gestures_chapter_skip,
                                    descriptionStringRes = R.string.player_gestures_chapter_skip_summary,
                                    dependencies = listOf(appPreferences.playerGestures),
                                    backendPreference = appPreferences.playerGesturesChapterSkip,
                                ),
                                PreferenceSwitch(
                                    nameStringResource = R.string.player_brightness_remember,
                                    dependencies = listOf(appPreferences.playerGestures, appPreferences.playerGesturesVB),
                                    backendPreference = appPreferences.playerGesturesBrightnessRemember,
                                ),
                                PreferenceSwitch(
                                    nameStringResource = R.string.player_start_maximized,
                                    descriptionStringRes = R.string.player_start_maximized_summary,
                                    dependencies = listOf(appPreferences.playerGestures),
                                    backendPreference = appPreferences.playerGesturesStartMaximized,
                                ),
                            ),
                        ),
                        PreferenceGroup(
                            nameStringResource = R.string.seeking,
                            preferences = listOf(
                                PreferenceLongInput(
                                    nameStringResource = R.string.seek_back_increment,
                                    backendPreference = appPreferences.playerSeekBackInc,
                                    suffixRes = R.string.ms,
                                ),
                                PreferenceLongInput(
                                    nameStringResource = R.string.seek_forward_increment,
                                    backendPreference = appPreferences.playerSeekForwardInc,
                                    suffixRes = R.string.ms,
                                ),
                                PreferenceSwitch(
                                    nameStringResource = R.string.pref_player_intro_skipper,
                                    descriptionStringRes = R.string.pref_player_intro_skipper_summary,
                                    backendPreference = appPreferences.playerIntroSkipper,
                                ),
                                PreferenceSwitch(
                                    nameStringResource = R.string.pref_player_chapter_markers,
                                    descriptionStringRes = R.string.pref_player_chapter_markers_summary,
                                    backendPreference = appPreferences.playerChapterMarkers,
                                ),
                            ),
                        ),
                        PreferenceGroup(
                            nameStringResource = R.string.trickplay,
                            preferences = listOf(
                                PreferenceSwitch(
                                    nameStringResource = R.string.pref_player_trickplay,
                                    descriptionStringRes = R.string.pref_player_trickplay_summary,
                                    backendPreference = appPreferences.playerTrickplay,
                                ),
                                PreferenceSwitch(
                                    nameStringResource = R.string.pref_player_gestures_seek_trickplay,
                                    descriptionStringRes = R.string.pref_player_gestures_seek_trickplay_summary,
                                    dependencies = listOf(appPreferences.playerTrickplay),
                                    backendPreference = appPreferences.playerGesturesSeekTrickplay,
                                ),
                            ),
                        ),
                        PreferenceGroup(
                            nameStringResource = R.string.picture_in_picture,
                            preferences = listOf(
                                PreferenceSwitch(
                                    nameStringResource = R.string.picture_in_picture_gesture,
                                    descriptionStringRes = R.string.picture_in_picture_gesture_summary,
                                    backendPreference = appPreferences.playerPipGesture,
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        ),
        PreferenceGroup(
            preferences = listOf(
                PreferenceCategory(
                    nameStringResource = R.string.users,
                    iconDrawableId = R.drawable.ic_user,
                    onClick = {
                        viewModelScope.launch {
                            eventsChannel.send(SettingsEvent.NavigateToUsers)
                        }
                    },
                ),
            ),
        ),
        PreferenceGroup(
            preferences = listOf(
                PreferenceCategory(
                    nameStringResource = R.string.settings_category_servers,
                    iconDrawableId = R.drawable.ic_server,
                    onClick = {
                        viewModelScope.launch {
                            eventsChannel.send(SettingsEvent.NavigateToServers)
                        }
                    },
                ),
            ),
        ),
        PreferenceGroup(
            preferences = listOf(
                PreferenceCategory(
                    nameStringResource = R.string.title_download,
                    iconDrawableId = R.drawable.ic_download,
                    onClick = {
                        viewModelScope.launch {
                            eventsChannel.send(SettingsEvent.NavigateToSettings(intArrayOf(it.nameStringResource)))
                        }
                    },
                    nestedPreferenceGroups = listOf(
                        PreferenceGroup(
                            preferences = listOf(
                                PreferenceSwitch(
                                    nameStringResource = R.string.download_mobile_data,
                                    backendPreference = appPreferences.downloadOverMobileData,
                                ),
                                PreferenceSwitch(
                                    nameStringResource = R.string.download_roaming,
                                    dependencies = listOf(appPreferences.downloadOverMobileData),
                                    backendPreference = appPreferences.downloadWhenRoaming,
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        ),
        PreferenceGroup(
            preferences = listOf(
                PreferenceCategory(
                    nameStringResource = R.string.settings_category_network,
                    iconDrawableId = R.drawable.ic_network,
                    onClick = {
                        viewModelScope.launch {
                            eventsChannel.send(SettingsEvent.NavigateToSettings(intArrayOf(it.nameStringResource)))
                        }
                    },
                    nestedPreferenceGroups = listOf(
                        PreferenceGroup(
                            preferences = listOf(
                                PreferenceLongInput(
                                    nameStringResource = R.string.settings_request_timeout,
                                    backendPreference = appPreferences.requestTimeout,
                                    suffixRes = R.string.ms,
                                ),
                                PreferenceLongInput(
                                    nameStringResource = R.string.settings_connect_timeout,
                                    backendPreference = appPreferences.connectTimeout,
                                    suffixRes = R.string.ms,
                                ),
                                PreferenceLongInput(
                                    nameStringResource = R.string.settings_socket_timeout,
                                    backendPreference = appPreferences.socketTimeout,
                                    suffixRes = R.string.ms,
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        ),
        PreferenceGroup(
            preferences = listOf(
                PreferenceCategory(
                    nameStringResource = R.string.settings_category_cache,
                    iconDrawableId = R.drawable.ic_hard_drive,
                    onClick = {
                        viewModelScope.launch {
                            eventsChannel.send(SettingsEvent.NavigateToSettings(intArrayOf(it.nameStringResource)))
                        }
                    },
                    nestedPreferenceGroups = listOf(
                        PreferenceGroup(
                            preferences = listOf(
                                PreferenceSwitch(
                                    nameStringResource = R.string.settings_use_cache_title,
                                    descriptionStringRes = R.string.settings_use_cache_summary,
                                    backendPreference = appPreferences.imageCache,
                                ),
                                PreferenceIntInput(
                                    nameStringResource = R.string.settings_cache_size,
                                    descriptionStringRes = R.string.settings_cache_size_message,
                                    dependencies = listOf(appPreferences.imageCache),
                                    backendPreference = appPreferences.imageCacheSize,
                                    suffixRes = R.string.mb,
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        ),
        PreferenceGroup(
            preferences = listOf(
                PreferenceCategory(
                    nameStringResource = R.string.about,
                    iconDrawableId = R.drawable.ic_info,
                    enabled = false,
                ),
            ),
        ),
    )

    fun loadPreferences(indexes: IntArray = intArrayOf()) {
        viewModelScope.launch {
            var preferences = topLevelPreferences

            // Show preferences based on the name of the parent
            for (index in indexes) {
                // If index is root (Settings) don't search for category
                if (index == R.string.title_settings) {
                    break
                }
                val preference = preferences.flatMap { it.preferences }.filterIsInstance<PreferenceCategory>().find { it.nameStringResource == index }
                if (preference != null) {
                    preferences = preference.nestedPreferenceGroups
                }
            }

            // Update all (visible) preferences with there current values
            preferences = preferences.map { preferenceGroup ->
                preferenceGroup.copy(
                    preferences = preferenceGroup.preferences.map { preference ->
                        when (preference) {
                            is PreferenceSwitch -> {
                                preference.copy(
                                    enabled = preference.enabled && preference.dependencies.all { appPreferences.getValue(it) },
                                    value = appPreferences.getValue(preference.backendPreference),
                                )
                            }
                            is PreferenceSelect -> {
                                preference.copy(
                                    enabled = preference.enabled && preference.dependencies.all { appPreferences.getValue(it) },
                                    value = appPreferences.getValue(preference.backendPreference),
                                )
                            }
                            is PreferenceIntInput -> {
                                preference.copy(
                                    enabled = preference.enabled && preference.dependencies.all { appPreferences.getValue(it) },
                                    value = appPreferences.getValue(preference.backendPreference),
                                )
                            }
                            is PreferenceLongInput -> {
                                preference.copy(
                                    enabled = preference.enabled && preference.dependencies.all { appPreferences.getValue(it) },
                                    value = appPreferences.getValue(preference.backendPreference),
                                )
                            }
                            else -> preference
                        }
                    },
                )
            }

            _state.emit(_state.value.copy(preferenceGroups = preferences))
        }
    }

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.OnUpdate -> {
                when (action.preference) {
                    is PreferenceSwitch -> appPreferences.setValue(action.preference.backendPreference, action.preference.value)
                    is PreferenceSelect -> appPreferences.setValue(action.preference.backendPreference, action.preference.value)
                    is PreferenceIntInput -> appPreferences.setValue(action.preference.backendPreference, action.preference.value)
                    is PreferenceLongInput -> appPreferences.setValue(action.preference.backendPreference, action.preference.value)
                }
            }
            else -> Unit
        }
    }
}
