package dev.jdtech.jellyfin.utils

import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import dev.jdtech.jellyfin.AppNavigationDirections
import dev.jdtech.jellyfin.models.View
import org.jellyfin.sdk.model.api.BaseItemDto
import timber.log.Timber

fun BaseItemDto.toView(): View {
    return View(
        id = id,
        name = name,
        type = collectionType
    )
}

fun Fragment.checkIfLoginRequired(error: String) {
    if (error.contains("401"))  {
        Timber.d("Login required!")
        findNavController().navigate(AppNavigationDirections.actionGlobalLoginFragment())
    }
}