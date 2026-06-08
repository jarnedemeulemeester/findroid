package dev.jdtech.jellyfin.settings.presentation.settings

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsFileEditViewModel @Inject constructor() : ViewModel() {
    fun loadFile() {

    }

    fun onAction(action: SettingsFileEditAction) {
        when (action) {
            SettingsFileEditAction.OnBackClick -> { /* TODO: Emit back event */ }
            SettingsFileEditAction.OnSave -> { /* Handle saving logic */ }
        }
    }
}