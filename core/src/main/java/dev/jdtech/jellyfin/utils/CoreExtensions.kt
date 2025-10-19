package dev.jdtech.jellyfin.utils

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.TypedValue
import androidx.annotation.AttrRes
import com.google.android.material.button.MaterialButton
import dev.jdtech.jellyfin.models.CollectionType
import dev.jdtech.jellyfin.models.JellyCastItem
import dev.jdtech.jellyfin.models.View
import org.jellyfin.sdk.model.DateTime
import org.jellyfin.sdk.model.api.BaseItemDto
import java.io.Serializable
import java.nio.charset.StandardCharsets
import java.text.DateFormat
import java.time.ZoneOffset
import java.util.Date

fun BaseItemDto.toView(items: List<JellyCastItem>): View {
    return View(
        id = id,
        name = name ?: "",
        items = items,
        type = CollectionType.fromString(collectionType?.serialName),
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

fun String.base64ToByteArray(): ByteArray {
    return Base64.decode(toByteArray(StandardCharsets.UTF_8), Base64.URL_SAFE or Base64.NO_WRAP)
}

fun ByteArray.toBase64Str(): String {
    return Base64.encodeToString(this, Base64.URL_SAFE or Base64.NO_WRAP)
}

fun DateTime.format(): String {
    val instant = this.toInstant(ZoneOffset.UTC)
    val date = Date.from(instant)
    return DateFormat.getDateInstance(DateFormat.SHORT).format(date)
}
