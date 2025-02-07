package dev.jdtech.jellyfin.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.AppPreferences
import dev.jdtech.jellyfin.utils.Downloader
import dev.jdtech.jellyfin.utils.restart
import dev.jdtech.jellyfin.utils.safeNavigate
import javax.inject.Inject
import dev.jdtech.jellyfin.core.R as CoreR

@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat() {
    @Inject
    lateinit var appPreferences: AppPreferences

    @Inject
    lateinit var downloader: Downloader

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(CoreR.xml.fragment_settings, rootKey)

        findPreference<Preference>("switchServer")?.setOnPreferenceClickListener {
            // findNavController().safeNavigate(TwoPaneSettingsFragmentDirections.actionNavigationSettingsToServerSelectFragment())
            true
        }

        findPreference<Preference>("switchUser")?.setOnPreferenceClickListener {
            val serverId = appPreferences.currentServer!!
            // findNavController().safeNavigate(TwoPaneSettingsFragmentDirections.actionNavigationSettingsToUsersFragment(serverId))
            true
        }

        findPreference<Preference>("switchAddress")?.setOnPreferenceClickListener {
            val serverId = appPreferences.currentServer!!
            findNavController().safeNavigate(TwoPaneSettingsFragmentDirections.actionNavigationSettingsToServerAddressesFragment(serverId))
            true
        }

        findPreference<Preference>("pref_offline_mode")?.setOnPreferenceClickListener {
            activity?.restart()
            true
        }

        findPreference<Preference>("privacyPolicy")?.setOnPreferenceClickListener {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://github.com/jarnedemeulemeester/findroid/blob/main/PRIVACY"),
            )
            startActivity(intent)
            true
        }

        findPreference<Preference>("appInfo")?.setOnPreferenceClickListener {
            findNavController().safeNavigate(TwoPaneSettingsFragmentDirections.actionSettingsFragmentToAboutLibraries())
            true
        }

        // Re-download pending items when a download preference has changed.
        // https://stackoverflow.com/questions/29476741/executing-code-after-a-preference-has-been-changed
        appPreferences.sharedPreferences.registerOnSharedPreferenceChangeListener { _, key ->
            if (key.startsWith("pref_downloads_")) {
                downloader.requeuePendingItems()
            }
        }
    }
}
