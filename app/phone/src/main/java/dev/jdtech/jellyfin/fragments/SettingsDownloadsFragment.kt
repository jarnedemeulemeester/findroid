package dev.jdtech.jellyfin.fragments

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import dev.jdtech.jellyfin.core.R as CoreR

class SettingsDownloadsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(CoreR.xml.fragment_settings_downloads, rootKey)
    }
}
