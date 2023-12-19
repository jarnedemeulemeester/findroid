package dev.jdtech.jellyfin.viewmodels

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

@HiltViewModel
class SettingsViewModel
@Inject
constructor(
    private val appPreferences: AppPreferences,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val eventsChannel = Channel<SettingsEvent>()
    val eventsChannelFlow = eventsChannel.receiveAsFlow()

    sealed class UiState {
        data class Normal(
            val preferences: List<Preference>,
        ) : UiState()

        data object Loading : UiState()
    }

    private val topLevelPreferences = listOf<Preference>(
        PreferenceCategory(
            nameStringResource = R.string.settings_category_language,
            iconDrawableId = R.drawable.ic_languages,
            onClick = {
                viewModelScope.launch {
                    eventsChannel.send(SettingsEvent.NavigateToSettings(intArrayOf(0), it.nameStringResource))
                }
            },
            nestedPreferences = listOf(
                PreferenceSelect(
                    nameStringResource = R.string.settings_preferred_audio_language,
                    iconDrawableId = R.drawable.ic_speaker,
                    backendName = Constants.PREF_AUDIO_LANGUAGE,
                    backendDefaultValue = null,
                    options = listOf(""),
                ),
                PreferenceSelect(
                    nameStringResource = R.string.settings_preferred_subtitle_language,
                    iconDrawableId = R.drawable.ic_closed_caption,
                    backendName = Constants.PREF_SUBTITLE_LANGUAGE,
                    backendDefaultValue = null,
                    options = listOf(""),
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
                    eventsChannel.send(SettingsEvent.NavigateToSettings(intArrayOf(2), it.nameStringResource))
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
                    options = listOf("no", "mediacodec", "mediacodec-copy"),
                ),
                PreferenceSelect(
                    nameStringResource = R.string.pref_player_mpv_vo,
                    dependencies = listOf(Constants.PREF_PLAYER_MPV),
                    backendName = Constants.PREF_PLAYER_MPV_VO,
                    backendDefaultValue = "gpu",
                    options = listOf("gpu", "gpu-next"),
                ),
                PreferenceSelect(
                    nameStringResource = R.string.pref_player_mpv_ao,
                    dependencies = listOf(Constants.PREF_PLAYER_MPV),
                    backendName = Constants.PREF_PLAYER_MPV_AO,
                    backendDefaultValue = "audiotrack",
                    options = listOf("audiotrack", "opensles"),
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
                    eventsChannel.send(SettingsEvent.NavigateToSettings(intArrayOf(7), it.nameStringResource))
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

            // Show preferences based on index (depth)
            for (index in indexes) {
                val preference = preferences[index]
                if (preference is PreferenceCategory) {
                    preferences = preference.nestedPreferences
                }
            }

            // Update all (visible) preferences with there current values
            preferences = preferences.map { preference ->
                when (preference) {
                    is PreferenceSwitch -> {
                        preference.copy(
                            enabled = preference.dependencies.all { getBoolean(it, false) },
                            value = getBoolean(preference.backendName, preference.backendDefaultValue),
                        )
                    }
                    is PreferenceSelect -> {
                        preference.copy(
                            enabled = preference.dependencies.all { getBoolean(it, false) },
                            value = getString(preference.backendName, preference.backendDefaultValue),
                        )
                    }
                    else -> preference
                }
            }

            _uiState.emit(UiState.Normal(preferences))
        }
    }

    private fun getBoolean(key: String, default: Boolean): Boolean {
        return appPreferences.getBoolean(key, default)
    }

    fun setBoolean(key: String, value: Boolean) {
        appPreferences.setBoolean(key, value)
    }

    private fun getString(key: String, default: String?): String? {
        return appPreferences.getString(key, default)
    }

    fun setString(key: String, value: String?) {
        appPreferences.setString(key, value)
    }
}

sealed interface SettingsEvent {
    data object NavigateToUsers : SettingsEvent
    data object NavigateToServers : SettingsEvent

    data class NavigateToSettings(val indexes: IntArray, val title: Int) : SettingsEvent
}
