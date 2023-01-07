package dev.jdtech.jellyfin.fragments

import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceHeaderFragmentCompat

class TwoPaneSettingsFragment : PreferenceHeaderFragmentCompat() {
    override fun onCreatePreferenceHeader(): PreferenceFragmentCompat {
        return SettingsFragment()
    }
}
