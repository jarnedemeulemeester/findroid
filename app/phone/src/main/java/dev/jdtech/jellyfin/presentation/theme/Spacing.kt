package dev.jdtech.jellyfin.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.compositionLocalOf
import dev.jdtech.jellyfin.core.presentation.theme.Spacings

val MaterialTheme.spacings
    get() = Spacings

val LocalSpacings = compositionLocalOf { MaterialTheme.spacings }
