package dev.jdtech.jellyfin.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.darkColorScheme as darkColorSchemeTv

val PrimaryDark = Color(0xffa1c9ff)
val OnPrimaryDark = Color(0xff00315e)
val PrimaryContainerDark = Color(0xff004884)
val OnPrimaryContainerDark = Color(0xffd3e4ff)
val Neutral900 = Color(0xff121A21)
val Neutral1000 = Color(0xff000000)

val Yellow = Color(0xFFF2C94C)

val ColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    surface = Neutral900,
    background = Neutral1000,
)

val ColorSchemeTv = darkColorSchemeTv(
    primary = ColorScheme.primary,
    onPrimary = ColorScheme.onPrimary,
    primaryContainer = ColorScheme.primaryContainer,
    onPrimaryContainer = ColorScheme.onPrimaryContainer,
    surface = ColorScheme.surface,
    background = ColorScheme.background,
)
