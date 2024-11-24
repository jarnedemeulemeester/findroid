package dev.jdtech.jellyfin.presentation.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import dev.jdtech.jellyfin.core.presentation.theme.Spacings
import androidx.tv.material3.LocalContentColor as LocalContentColorTv
import androidx.tv.material3.MaterialTheme as MaterialThemeTv
import androidx.tv.material3.contentColorFor as contentColorForTv

@Composable
fun FindroidTheme(
    content: @Composable BoxScope.() -> Unit,
) {
    MaterialTheme(
        colorScheme = darkScheme,
        typography = Typography,
        shapes = shapes,
    ) {
        MaterialThemeTv(
            colorScheme = darkSchemeTv,
            typography = TypographyTv,
            shapes = shapesTv,
        ) {
            CompositionLocalProvider(
                LocalContentColor provides contentColorFor(MaterialTheme.colorScheme.background),
                LocalContentColorTv provides contentColorForTv(MaterialThemeTv.colorScheme.background),
                LocalSpacings provides Spacings,
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
                ) {
                    content()
                }
            }
        }
    }
}
