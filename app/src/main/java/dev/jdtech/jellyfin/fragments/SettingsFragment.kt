package dev.jdtech.jellyfin.fragments

import android.os.Bundle
import androidx.navigation.fragment.findNavController
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
    }
}