package dev.jdtech.jellyfin.fragments

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import dev.jdtech.jellyfin.R

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.fragment_settings, rootKey)
    }
}