package dev.jdtech.jellyfin.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme as MaterialThemeTv

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FindroidTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = ColorScheme,
        typography = Typography,
        shapes = shapes,
    ) {
        CompositionLocalProvider(
            LocalSpacings provides Spacings(),
        ) {
            MaterialThemeTv(
                colorScheme = ColorSchemeTv,
                typography = TypographyTv,
                shapes = shapesTv,
                content = content,
            )
        }
    }
}
