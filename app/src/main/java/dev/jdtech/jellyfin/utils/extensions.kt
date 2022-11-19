package dev.jdtech.jellyfin.utils

import android.content.res.ColorStateList
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.widget.ImageButton
import androidx.annotation.AttrRes
import androidx.annotation.ColorRes
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import dev.jdtech.jellyfin.AppNavigationDirections
import dev.jdtech.jellyfin.models.View
import java.io.Serializable
import org.jellyfin.sdk.model.api.BaseItemDto
import timber.log.Timber

fun BaseItemDto.toView(): View {
    return View(
        id = id,
        name = name,
        type = collectionType
    )
}

fun Fragment.checkIfLoginRequired(error: String?) {
    if (error != null) {
        if (error.contains("401")) {
            Timber.d("Login required!")
            findNavController().navigate(AppNavigationDirections.actionGlobalLoginFragment(reLogin = true))
        }
    }
}

fun Resources.dip(px: Int) = (px * displayMetrics.density).toInt()

fun ImageButton.setTintColor(@ColorRes colorId: Int, theme: Resources.Theme) {
    this.imageTintList = ColorStateList.valueOf(
        resources.getColor(
            colorId,
            theme
        )
    )
}

fun ImageButton.setTintColorAttribute(@AttrRes attributeId: Int, theme: Resources.Theme) {
    val typedValue = TypedValue()
    theme.resolveAttribute(attributeId, typedValue, true)
    this.imageTintList = ColorStateList.valueOf(
        resources.getColor(
            typedValue.resourceId,
            theme
        )
    )
}

inline fun <reified T : Serializable> Bundle.serializable(key: String): T? = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> getSerializable(key, T::class.java)
    else -> @Suppress("DEPRECATION") getSerializable(key) as? T
}
