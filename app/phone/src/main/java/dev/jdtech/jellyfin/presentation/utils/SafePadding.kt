package dev.jdtech.jellyfin.presentation.utils

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass

data class SafePadding(val start: Dp, val top: Dp, val end: Dp, val bottom: Dp)

@Composable
fun rememberSafePadding(
    handleStartInsets: Boolean = true,
    handleBottomInsets: Boolean = true,
    handleImeInsets: Boolean = false,
): SafePadding {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass

    val safeInsets = if (handleImeInsets) {
        WindowInsets.safeDrawing
    } else {
        WindowInsets.safeDrawing.exclude(WindowInsets.ime)
    }

    val safePaddingStart =
        if (handleStartInsets) {
            with(density) { safeInsets.getLeft(this, layoutDirection).toDp() }
        } else {
            // Navigation rail handles safe drawing inset in medium and expanded width
            when {
                windowSizeClass.isWidthAtLeastBreakpoint(
                    WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND
                ) -> 0.dp

                windowSizeClass.isWidthAtLeastBreakpoint(
                    WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND
                ) -> 0.dp

                else ->
                    with(density) { safeInsets.getLeft(this, layoutDirection).toDp() }
            }
        }

    val safePaddingTop = with(density) { safeInsets.getTop(this).toDp() }
    val safePaddingEnd =
        with(density) { safeInsets.getRight(this, layoutDirection).toDp() }
    val safePaddingBottom = if (handleBottomInsets) {
        with(density) { safeInsets.getBottom(this).toDp() }
    } else {
        when {
            windowSizeClass.isWidthAtLeastBreakpoint(
                WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND
            ) -> with(density) { safeInsets.getBottom(this).toDp() }

            else ->
                0.dp
        }
    }

    return SafePadding(
        start = safePaddingStart,
        top = safePaddingTop,
        end = safePaddingEnd,
        bottom = safePaddingBottom,
    )
}
