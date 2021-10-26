package dev.jdtech.jellyfin.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
import androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.viewmodels.SettingsViewModel

@AndroidEntryPoint
class SettingsFragment: PreferenceFragmentCompat() {

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.fragment_settings, rootKey)

        findPreference<Preference>("switchServer")?.setOnPreferenceClickListener {
            findNavController().navigate(SettingsFragmentDirections.actionNavigationSettingsToServerSelectFragment2())
            true
        }

        findPreference<ListPreference>("theme")?.setOnPreferenceChangeListener { _, newValue ->
            when (newValue) {
                "system" -> setDefaultNightMode(MODE_NIGHT_FOLLOW_SYSTEM)
                "light" -> setDefaultNightMode(MODE_NIGHT_NO)
                "dark" -> setDefaultNightMode(MODE_NIGHT_YES)
            }
            true
        }

        findPreference<Preference>("privacyPolicy")?.setOnPreferenceClickListener {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://github.com/jarnedemeulemeester/findroid/blob/main/PRIVACY")
            )
            startActivity(intent)
            true
        }

        findPreference<Preference>("appInfo")?.setOnPreferenceClickListener {
            findNavController().navigate(SettingsFragmentDirections.actionSettingsFragmentToAboutLibraries())
            true
        }

        findPreference<EditTextPreference>("deviceName")?.setOnPreferenceChangeListener { _, name ->
            viewModel.updateDeviceName(name.toString())
            true
        }
    }
}