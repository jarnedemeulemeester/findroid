package dev.jdtech.jellyfin.viewmodels

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.AppPreferences
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
@Inject
constructor(
    private val appPreferences: AppPreferences,
) : ViewModel() {
    fun getBoolean(key: String, default: Boolean): Boolean {
        return appPreferences.getBoolean(key, default)
    }

    fun setBoolean(key: String, value: Boolean) {
        appPreferences.setBoolean(key, value)
    }

    fun getString(key: String, default: String?): String? {
        return appPreferences.getString(key, default)
    }

    fun setString(key: String, value: String?) {
        appPreferences.setString(key, value)
    }
}
