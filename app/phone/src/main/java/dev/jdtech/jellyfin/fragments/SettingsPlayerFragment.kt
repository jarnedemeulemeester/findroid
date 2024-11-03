package dev.jdtech.jellyfin.fragments

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import dev.jdtech.jellyfin.core.R as CoreR

class SettingsPlayerFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(CoreR.xml.fragment_settings_player, rootKey)
        findPreference<EditTextPreference>("pref_player_seek_back_inc")?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
        }
        findPreference<EditTextPreference>("pref_player_seek_forward_inc")?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
        }
        findPreference<Preference>("pref_player_subtitles")?.setOnPreferenceClickListener {
            startActivity(Intent(Settings.ACTION_CAPTIONING_SETTINGS))
            true
        }

        // Media Segments - Skip Button
        val skipButtonTypePreference = findPreference<MultiSelectListPreference>("pref_player_media_segments_skip_button_type")

        skipButtonTypePreference?.let {
            setupMultiSelectPreference(it)
        }

        findPreference<EditTextPreference>("pref_player_media_segments_skip_button_duration")?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
        }

        // Media Segments - Auto Skip
        val autoSkipPreference = findPreference<ListPreference>("pref_player_media_segments_auto_skip")
        val autoSkipTypePreference = findPreference<MultiSelectListPreference>("pref_player_media_segments_auto_skip_type")

        autoSkipTypePreference?.let {
            setupMultiSelectPreference(it)
            it.isEnabled = autoSkipPreference?.value != "never"
        }
        autoSkipPreference?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            val isEnabled = newValue != "never" // Enable if value is not "never"
            autoSkipTypePreference?.isEnabled = isEnabled
            true
        }

        findPreference<EditTextPreference>("pref_player_media_segments_next_episode_threshold")?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
        }
    }

    private fun setupMultiSelectPreference(preference: MultiSelectListPreference) {
        preference.summary = createSummary(preference.values)
        preference.setOnPreferenceChangeListener { _, newValue ->
            preference.summary = createSummary(newValue as Set<*>)
            true
        }
    }

    private val valueToDisplayMap: Map<String, String> by lazy {
        val values = resources.getStringArray(CoreR.array.media_segments_type_values)
        val displays = resources.getStringArray(CoreR.array.media_segments_type)
        values.zip(displays).toMap()
    }

    private fun createSummary(selectedValues: Set<*>): String {
        return if (selectedValues.isEmpty()) {
            getString(CoreR.string.media_segments_type_summary_none)
        } else {
            selectedValues.map { value ->
                valueToDisplayMap[value] ?: value
            }.joinToString(", ")
        }
    }
}
