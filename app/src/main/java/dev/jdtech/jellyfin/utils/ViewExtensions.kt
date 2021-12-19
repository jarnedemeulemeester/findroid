package dev.jdtech.jellyfin.utils

import android.view.View
import androidx.core.view.isVisible

fun View.toggleVisibility() {
    isVisible = !isVisible
}
