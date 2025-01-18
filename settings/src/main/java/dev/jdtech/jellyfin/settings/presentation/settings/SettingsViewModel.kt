package dev.jdtech.jellyfin.settings.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.AppPreferences
import dev.jdtech.jellyfin.Constants
import dev.jdtech.jellyfin.core.R
import dev.jdtech.jellyfin.models.Preference
import dev.jdtech.jellyfin.models.PreferenceCategory
import dev.jdtech.jellyfin.models.PreferenceSelect
import dev.jdtech.jellyfin.models.PreferenceSwitch
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

    private val topLevelPreferences = listOf<Preference>(
        PreferenceCategory(
            nameStringResource = R.string.settings_category_language,
            iconDrawableId = R.drawable.ic_languages,
            onClick = {
                viewModelScope.launch {
                    eventsChannel.send(SettingsEvent.NavigateToSettings(intArrayOf(it.nameStringResource)))
                }
            },
            nestedPreferences = listOf(
                PreferenceSelect(
                    nameStringResource = R.string.settings_preferred_audio_language,
                    iconDrawableId = R.drawable.ic_speaker,
                    backendName = Constants.PREF_AUDIO_LANGUAGE,
                    backendDefaultValue = null,
                    options = R.array.languages,
                    optionValues = R.array.languages_values,
                ),
                PreferenceSelect(
                    nameStringResource = R.string.settings_preferred_subtitle_language,
                    iconDrawableId = R.drawable.ic_closed_caption,
                    backendName = Constants.PREF_SUBTITLE_LANGUAGE,
                    backendDefaultValue = null,
                    options = R.array.languages,
                    optionValues = R.array.languages_values,
                ),
            ),
        ),
        PreferenceCategory(
            nameStringResource = R.string.settings_category_appearance,
            iconDrawableId = R.drawable.ic_palette,
            enabled = false,
        ),
        PreferenceCategory(
            nameStringResource = R.string.settings_category_player,
            iconDrawableId = R.drawable.ic_play,
            onClick = {
                viewModelScope.launch {
                    eventsChannel.send(SettingsEvent.NavigateToSettings(intArrayOf(it.nameStringResource)))
                }
            },
            nestedPreferences = listOf(
                PreferenceSwitch(
                    nameStringResource = R.string.mpv_player,
                    descriptionStringRes = R.string.mpv_player_summary,
                    backendName = Constants.PREF_PLAYER_MPV,
                    backendDefaultValue = false,
                ),
                PreferenceSelect(
                    nameStringResource = R.string.pref_player_mpv_hwdec,
                    dependencies = listOf(Constants.PREF_PLAYER_MPV),
                    backendName = Constants.PREF_PLAYER_MPV_HWDEC,
                    backendDefaultValue = "mediacodec",
                    options = R.array.mpv_hwdec,
                    optionValues = R.array.mpv_hwdec,
                ),
                PreferenceSelect(
                    nameStringResource = R.string.pref_player_mpv_vo,
                    dependencies = listOf(Constants.PREF_PLAYER_MPV),
                    backendName = Constants.PREF_PLAYER_MPV_VO,
                    backendDefaultValue = "gpu-next",
                    options = R.array.mpv_vos,
                    optionValues = R.array.mpv_vos,
                ),
                PreferenceSelect(
                    nameStringResource = R.string.pref_player_mpv_ao,
                    dependencies = listOf(Constants.PREF_PLAYER_MPV),
                    backendName = Constants.PREF_PLAYER_MPV_AO,
                    backendDefaultValue = "audiotrack",
                    options = R.array.mpv_aos,
                    optionValues = R.array.mpv_aos,
                ),
            ),
        ),
        PreferenceCategory(
            nameStringResource = R.string.users,
            iconDrawableId = R.drawable.ic_user,
            onClick = {
                viewModelScope.launch {
                    eventsChannel.send(SettingsEvent.NavigateToUsers)
                }
            },
        ),
        PreferenceCategory(
            nameStringResource = R.string.settings_category_servers,
            iconDrawableId = R.drawable.ic_server,
            onClick = {
                viewModelScope.launch {
                    eventsChannel.send(SettingsEvent.NavigateToServers)
                }
            },
        ),
        PreferenceCategory(
            nameStringResource = R.string.settings_category_device,
            iconDrawableId = R.drawable.ic_smartphone,
            enabled = false,
        ),
        PreferenceCategory(
            nameStringResource = R.string.settings_category_network,
            iconDrawableId = R.drawable.ic_network,
            enabled = false,
        ),
        PreferenceCategory(
            nameStringResource = R.string.settings_category_cache,
            iconDrawableId = R.drawable.ic_hard_drive,
            onClick = {
                viewModelScope.launch {
                    eventsChannel.send(SettingsEvent.NavigateToSettings(intArrayOf(it.nameStringResource)))
                }
            },
            nestedPreferences = listOf(
                PreferenceSwitch(
                    nameStringResource = R.string.settings_use_cache_title,
                    descriptionStringRes = R.string.settings_use_cache_summary,
                    backendName = Constants.PREF_IMAGE_CACHE,
                    backendDefaultValue = true,
                ),
            ),
        ),
        PreferenceCategory(
            nameStringResource = R.string.about,
            iconDrawableId = R.drawable.ic_info,
            enabled = false,
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
                val preference = preferences.filterIsInstance<PreferenceCategory>().find { it.nameStringResource == index }
                if (preference != null) {
                    preferences = preference.nestedPreferences
                }
            }

            // Update all (visible) preferences with there current values
            preferences = preferences.map { preference ->
                when (preference) {
                    is PreferenceSwitch -> {
                        preference.copy(
                            enabled = preference.dependencies.all { appPreferences.getBoolean(it, false) },
                            value = appPreferences.getBoolean(preference.backendName, preference.backendDefaultValue),
                        )
                    }
                    is PreferenceSelect -> {
                        preference.copy(
                            enabled = preference.dependencies.all { appPreferences.getBoolean(it, false) },
                            value = appPreferences.getString(preference.backendName, preference.backendDefaultValue),
                        )
                    }
                    else -> preference
                }
            }

            _state.emit(_state.value.copy(preferences = preferences))
        }
    }

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.OnUpdate -> {
                when (action.preference) {
                    is PreferenceSwitch -> appPreferences.setBoolean(action.preference.backendName, action.preference.value)
                    is PreferenceSelect -> appPreferences.setString(action.preference.backendName, action.preference.value)
                }
            }
            else -> Unit
        }
    }
}
