package dev.jdtech.jellyfin.fragments

import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate.*
import androidx.navigation.fragment.findNavController
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import dev.jdtech.jellyfin.R

class SettingsFragment : PreferenceFragmentCompat() {
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
    }
}