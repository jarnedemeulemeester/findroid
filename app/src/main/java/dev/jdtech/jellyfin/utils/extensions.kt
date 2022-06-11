package dev.jdtech.jellyfin.utils

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.util.TypedValue
import android.widget.ImageButton
import android.widget.Toast
import androidx.annotation.AttrRes
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
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

fun Fragment.checkIfLoginRequired(error: String?) {
    if (error != null) {
        if (error.contains("401"))  {
            Timber.d("Login required!")
            findNavController().navigate(AppNavigationDirections.actionGlobalLoginFragment())
        }
    }
}

inline fun Context.toast(@StringRes text: Int, duration: Int = Toast.LENGTH_SHORT) =
    Toast.makeText(this, text, duration).show()


inline fun Resources.dip(px: Int) = (px * displayMetrics.density).toInt()

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