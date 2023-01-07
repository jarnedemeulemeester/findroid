package dev.jdtech.jellyfin.fragments

import android.os.Bundle
import android.text.InputType
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.utils.Constants

@Suppress("unused")
class SettingsNetworkFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.fragment_settings_network, rootKey)

        findPreference<EditTextPreference>(Constants.PREF_NETWORK_SOCKET_TIMEOUT)?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
        }
    }
}
