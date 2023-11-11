package dev.jdtech.jellyfin.utils

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import androidx.annotation.AttrRes
import com.google.android.material.button.MaterialButton
import dev.jdtech.jellyfin.models.CollectionType
import dev.jdtech.jellyfin.models.View
import org.jellyfin.sdk.model.api.BaseItemDto
import java.io.Serializable
import dev.jdtech.jellyfin.core.R as CoreR

fun BaseItemDto.toView(): View {
    return View(
        id = id,
        name = name ?: "",
        type = CollectionType.fromString(collectionType),
    )
}

fun Resources.dip(px: Int) = (px * displayMetrics.density).toInt()

fun MaterialButton.setIconTintColorAttribute(@AttrRes attributeId: Int, theme: Resources.Theme) {
    val typedValue = TypedValue()
    theme.resolveAttribute(attributeId, typedValue, true)
    this.iconTint = ColorStateList.valueOf(
        resources.getColor(
            typedValue.resourceId,
            theme,
        ),
    )
}

inline fun <reified T : Serializable> Bundle.serializable(key: String): T? = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> getSerializable(key, T::class.java)
    else ->
        @Suppress("DEPRECATION")
        getSerializable(key)
            as? T
}

fun Activity.restart() {
    val intent = Intent(this, this::class.java)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    startActivity(intent)
}

fun Long.formatDuration(resources: Resources): String {
    val hours = (this / 3600L).toInt()
    val minutes = ((this % 3600L) / 60L).toInt()
    val seconds = ((this % 3600L) % 60L).toInt()

    val stringParts = mutableListOf<String>()

    if (hours > 0) {
        val hourText = resources.getQuantityString(
            CoreR.plurals.hour,
            hours,
        )
        stringParts.add("$hours $hourText")
    }

    if (minutes > 0) {
        val minuteText = resources.getQuantityString(
            CoreR.plurals.minute,
            minutes,
        )
        stringParts.add("$minutes $minuteText")
    }

    if (seconds > 0) {
        val secondText = resources.getQuantityString(
            CoreR.plurals.second,
            seconds,
        )
        stringParts.add("$seconds $secondText")
    }

    return stringParts.joinToString(" ")
}
