package dev.jdtech.jellyfin.settings.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.core.R
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import dev.jdtech.jellyfin.settings.presentation.models.PreferenceCategory
import dev.jdtech.jellyfin.settings.presentation.models.PreferenceGroup
import dev.jdtech.jellyfin.settings.presentation.models.PreferenceNumberInput
import dev.jdtech.jellyfin.settings.presentation.models.PreferenceSelect
import dev.jdtech.jellyfin.settings.presentation.models.PreferenceSwitch
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import dev.jdtech.jellyfin.core.R as CoreR

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
                                PreferenceSelect(
                                    nameStringResource = R.string.settings_preferred_audio_language,
                                    iconDrawableId = R.drawable.ic_speaker,
                                    backendName = appPreferences.preferredAudioLanguage.backendName,
                                    options = R.array.languages,
                                    optionValues = R.array.languages_values,
                                    optionsIncludeNull = true,
                                ),
                                PreferenceSelect(
                                    nameStringResource = R.string.settings_preferred_subtitle_language,
                                    iconDrawableId = R.drawable.ic_closed_caption,
                                    backendName = appPreferences.preferredSubtitleLanguage.backendName,
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
                    enabled = false,
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
                                    backendName = appPreferences.playerMpv.backendName,
                                ),
                                PreferenceSelect(
                                    nameStringResource = R.string.pref_player_mpv_hwdec,
                                    dependencies = listOf(appPreferences.playerMpv.backendName),
                                    backendName = appPreferences.playerMpvHwdec.backendName,
                                    options = R.array.mpv_hwdec,
                                    optionValues = R.array.mpv_hwdec,
                                ),
                                PreferenceSelect(
                                    nameStringResource = R.string.pref_player_mpv_vo,
                                    dependencies = listOf(appPreferences.playerMpv.backendName),
                                    backendName = appPreferences.playerMpvVo.backendName,
                                    options = R.array.mpv_vos,
                                    optionValues = R.array.mpv_vos,
                                ),
                                PreferenceSelect(
                                    nameStringResource = R.string.pref_player_mpv_ao,
                                    dependencies = listOf(appPreferences.playerMpv.backendName),
                                    backendName = appPreferences.playerMpvAo.backendName,
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
                                    backendName = appPreferences.playerGestures.backendName,
                                ),
                                PreferenceSwitch(
                                    nameStringResource = R.string.player_gestures_vb,
                                    descriptionStringRes = R.string.player_gestures_vb_summary,
                                    dependencies = listOf(appPreferences.playerGestures.backendName),
                                    backendName = appPreferences.playerGesturesVB.backendName,
                                ),
                                PreferenceSwitch(
                                    nameStringResource = R.string.player_gestures_seek,
                                    descriptionStringRes = R.string.player_gestures_seek_summary,
                                    dependencies = listOf(appPreferences.playerGestures.backendName),
                                    backendName = appPreferences.playerGesturesSeek.backendName,
                                ),
                                PreferenceSwitch(
                                    nameStringResource = R.string.player_gestures_zoom,
                                    descriptionStringRes = R.string.player_gestures_zoom_summary,
                                    dependencies = listOf(appPreferences.playerGestures.backendName),
                                    backendName = appPreferences.playerGesturesZoom.backendName,
                                ),
                                PreferenceSwitch(
                                    nameStringResource = R.string.player_gestures_chapter_skip,
                                    descriptionStringRes = R.string.player_gestures_chapter_skip_summary,
                                    dependencies = listOf(appPreferences.playerGestures.backendName),
                                    backendName = appPreferences.playerGesturesChapterSkip.backendName,
                                ),
                                PreferenceSwitch(
                                    nameStringResource = R.string.player_brightness_remember,
                                    dependencies = listOf(appPreferences.playerGestures.backendName, appPreferences.playerGesturesVB.backendName),
                                    backendName = appPreferences.playerGesturesBrightnessRemember.backendName,
                                ),
                                PreferenceSwitch(
                                    nameStringResource = R.string.player_start_maximized,
                                    descriptionStringRes = R.string.player_start_maximized_summary,
                                    dependencies = listOf(appPreferences.playerGestures.backendName),
                                    backendName = appPreferences.playerGesturesStartMaximized.backendName,
                                ),
                            ),
                        ),
                        PreferenceGroup(
                            nameStringResource = R.string.seeking,
                            preferences = listOf(
                                // TODO: add support for longs
//                                PreferenceNumberInput(
//                                    nameStringResource = CoreR.string.seek_back_increment,
//                                    backendName = appPreferences.playerSeekBackInc.backendName,
//                                    suffix = "ms"
//                                ),
//                                PreferenceNumberInput(
//                                    nameStringResource = CoreR.string.seek_forward_increment,
//                                    backendName = appPreferences.playerSeekForwardInc.backendName,
//                                    suffix = "ms"
//                                ),
                                PreferenceSwitch(
                                    nameStringResource = R.string.pref_player_intro_skipper,
                                    descriptionStringRes = R.string.pref_player_intro_skipper_summary,
                                    backendName = appPreferences.playerIntroSkipper.backendName,
                                ),
                                PreferenceSwitch(
                                    nameStringResource = R.string.pref_player_chapter_markers,
                                    descriptionStringRes = R.string.pref_player_chapter_markers_summary,
                                    backendName = appPreferences.playerChapterMarkers.backendName,
                                ),
                            ),
                        ),
                        PreferenceGroup(
                            nameStringResource = R.string.trickplay,
                            preferences = listOf(
                                PreferenceSwitch(
                                    nameStringResource = R.string.pref_player_trickplay,
                                    descriptionStringRes = R.string.pref_player_trickplay_summary,
                                    backendName = appPreferences.playerTrickplay.backendName,
                                ),
                                PreferenceSwitch(
                                    nameStringResource = R.string.pref_player_gestures_seek_trickplay,
                                    descriptionStringRes = R.string.pref_player_gestures_seek_trickplay_summary,
                                    dependencies = listOf(appPreferences.playerTrickplay.backendName),
                                    backendName = appPreferences.playerGesturesSeekTrickplay.backendName,
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
                                    backendName = appPreferences.downloadOverMobileData.backendName,
                                ),
                                PreferenceSwitch(
                                    nameStringResource = R.string.download_roaming,
                                    dependencies = listOf(appPreferences.downloadOverMobileData.backendName),
                                    backendName = appPreferences.downloadWhenRoaming.backendName,
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
                    nameStringResource = R.string.settings_category_device,
                    iconDrawableId = R.drawable.ic_smartphone,
                    enabled = false,
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
//                                PreferenceNumberInput(
//                                    nameStringResource = CoreR.string.settings_request_timeout,
//                                    backendName = appPreferences.requestTimeout.backendName,
//                                    suffix = "ms"
//                                ),
//                                PreferenceNumberInput(
//                                    nameStringResource = CoreR.string.settings_connect_timeout,
//                                    backendName = appPreferences.connectTimeout.backendName,
//                                    suffix = "ms"
//                                ),
//                                PreferenceNumberInput(
//                                    nameStringResource = CoreR.string.settings_socket_timeout,
//                                    backendName = appPreferences.socketTimeout.backendName,
//                                    suffix = "ms"
//                                )
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
                                    backendName = appPreferences.imageCache.backendName,
                                ),
                                PreferenceNumberInput(
                                    nameStringResource = CoreR.string.settings_cache_size,
                                    descriptionStringRes = CoreR.string.settings_cache_size_message,
                                    dependencies = listOf(appPreferences.imageCache.backendName),
                                    backendName = appPreferences.imageCacheSize.backendName,
                                    suffix = "MB",
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
                if (index == CoreR.string.title_settings) {
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
                                    enabled = preference.dependencies.all { appPreferences.getValue(it) },
                                    value = appPreferences.getValue(preference.backendName),
                                )
                            }
                            is PreferenceSelect -> {
                                preference.copy(
                                    enabled = preference.dependencies.all { appPreferences.getValue(it) },
                                    value = appPreferences.getValue(preference.backendName),
                                )
                            }
                            is PreferenceNumberInput -> {
                                preference.copy(
                                    enabled = preference.dependencies.all { appPreferences.getValue(it) },
                                    value = appPreferences.getValue(preference.backendName),
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
                    is PreferenceSwitch -> appPreferences.setValue(action.preference.backendName, action.preference.value)
                    is PreferenceSelect -> appPreferences.setValue(action.preference.backendName, action.preference.value)
                }
            }
            else -> Unit
        }
    }
}
