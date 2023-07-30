package dev.jdtech.jellyfin.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme

@Immutable
data class Spacings(
    val default: Dp = 24.dp,
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 16.dp,
    val large: Dp = 32.dp,
    val extraLarge: Dp = 64.dp,
)

@OptIn(ExperimentalTvMaterial3Api::class)
val MaterialTheme.spacings
    get() = Spacings()

@OptIn(ExperimentalTvMaterial3Api::class)
val LocalSpacings = compositionLocalOf { MaterialTheme.spacings }
