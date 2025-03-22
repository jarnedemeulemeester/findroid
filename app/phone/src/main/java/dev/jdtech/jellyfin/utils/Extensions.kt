package dev.jdtech.jellyfin.utils

import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.NavOptions
import org.jellyfin.sdk.model.DateTime
import timber.log.Timber
import java.text.DateFormat
import java.time.ZoneOffset
import java.util.Date

fun Fragment.checkIfLoginRequired(error: String?) {
    if (error != null) {
        if (error.contains("401")) {
            Timber.d("Login required!")
            // findNavController().safeNavigate(AppNavigationDirections.actionGlobalLoginFragment(reLogin = true))
        }
    }
}

fun NavController.safeNavigate(directions: NavDirections, navOptions: NavOptions? = null) {
    try {
        navigate(directions, navOptions)
    } catch (e: IllegalArgumentException) {
        Timber.e(e, "Failed to navigate")
    }
}

fun DateTime.format(): String {
    val instant = this.toInstant(ZoneOffset.UTC)
    val date = Date.from(instant)
    return DateFormat.getDateInstance(DateFormat.SHORT).format(date)
}
