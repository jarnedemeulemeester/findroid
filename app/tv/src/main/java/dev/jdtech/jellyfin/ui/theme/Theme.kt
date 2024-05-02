package dev.jdtech.jellyfin.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.MaterialTheme as MaterialThemeTv

@Composable
fun FindroidTheme(
    content: @Composable BoxScope.() -> Unit,
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
                content = {
                    Surface(
                        colors = SurfaceDefaults.colors(
                            containerColor = androidx.tv.material3.MaterialTheme.colorScheme.background,
                        ),
                        shape = RectangleShape,
                    ) {
                        Box(
                            modifier = Modifier.background(
                                Brush.linearGradient(
                                    listOf(
                                        Color.Black,
                                        Color(0xFF001721),
                                    ),
                                ),
                            ),
                            content = content,
                        )
                    }
                },
            )
        }
    }
}
