package dev.jdtech.jellyfin.fragments

import android.os.Bundle
import androidx.fragment.app.viewModels
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.viewmodels.SettingsDeviceViewModel

@AndroidEntryPoint
@Suppress("unused")
class SettingsDeviceFragment : PreferenceFragmentCompat() {

    private val viewModel: SettingsDeviceViewModel by viewModels()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.fragment_settings_device, rootKey)

        findPreference<EditTextPreference>("deviceName")?.setOnPreferenceChangeListener { _, name ->
            viewModel.updateDeviceName(name.toString())
            true
        }
    }
}