package dev.jdtech.jellyfin.viewmodels

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.AppPreferences
import dev.jdtech.jellyfin.models.Preference
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
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

    fun loadPreferences(indexes: IntArray = intArrayOf()) {}

    fun setBoolean(key: String, value: Boolean) {
        appPreferences.setBoolean(key, value)
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
