package dev.jdtech.jellyfin.ui.theme

import androidx.compose.runtime.Composable
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

@OptIn(ExperimentalTvMaterial3Api::class)
private val ColorSchemeTv = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    surface = Neutral900,
    background = Neutral1000,
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FindroidTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = ColorSchemeTv,
        typography = TypographyTv,
        shapes = shapesTv,
        content = content,
    )
}
