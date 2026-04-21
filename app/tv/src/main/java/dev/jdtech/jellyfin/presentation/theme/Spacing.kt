package dev.jdtech.jellyfin.presentation.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.tv.material3.MaterialTheme
import dev.jdtech.jellyfin.core.presentation.theme.Spacings

val MaterialTheme.spacings
    get() = Spacings

val LocalSpacings = compositionLocalOf { MaterialTheme.spacings }
