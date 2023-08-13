package dev.jdtech.jellyfin.utils

import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import dev.jdtech.jellyfin.AppNavigationDirections
import timber.log.Timber

fun Fragment.checkIfLoginRequired(error: String?) {
    if (error != null) {
        if (error.contains("401")) {
            Timber.d("Login required!")
            findNavController().navigate(AppNavigationDirections.actionGlobalLoginFragment(reLogin = true))
        }
    }
}
