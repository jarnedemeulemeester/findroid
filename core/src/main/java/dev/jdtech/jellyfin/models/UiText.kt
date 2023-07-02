package dev.jdtech.jellyfin.models

import android.content.res.Resources
import androidx.annotation.StringRes

sealed class UiText {
    data class DynamicString(val value: String) : UiText()
    class StringResource(
        @StringRes val resId: Int,
        vararg val args: Any?,
    ) : UiText()

    fun asString(resources: Resources): String {
        return when (this) {
            is DynamicString -> return value
            is StringResource -> resources.getString(resId, *args)
        }
    }
}
