package dev.jdtech.jellyfin.presentation.utils

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowWidthSizeClass

data class SafePadding(
    val start: Dp,
    val top: Dp,
    val end: Dp,
    val bottom: Dp,
)

@Composable
fun rememberSafePadding(
    handleStartInsets: Boolean = true,
): SafePadding {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass

    val safePaddingStart = if (handleStartInsets) {
        with(density) { WindowInsets.safeDrawing.getLeft(this, layoutDirection).toDp() }
    } else {
        // Navigation rail handles safe drawing inset in medium and expanded width
        when (windowSizeClass.windowWidthSizeClass) {
            WindowWidthSizeClass.EXPANDED -> 0.dp
            WindowWidthSizeClass.MEDIUM -> 0.dp
            else -> with(density) { WindowInsets.safeDrawing.getLeft(this, layoutDirection).toDp() }
        }
    }

    val safePaddingTop = with(density) { WindowInsets.safeDrawing.getTop(this).toDp() }
    val safePaddingEnd = with(density) { WindowInsets.safeDrawing.getRight(this, layoutDirection).toDp() }
    val safePaddingBottom = with(density) { WindowInsets.safeDrawing.getBottom(this).toDp() }

    return SafePadding(
        start = safePaddingStart,
        top = safePaddingTop,
        end = safePaddingEnd,
        bottom = safePaddingBottom,
    )
}
