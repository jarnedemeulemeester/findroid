package dev.jdtech.jellyfin.presentation.setup

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

fun Modifier.backgroundGradient(colors: List<Color>) =
    drawBehind {
        drawRect(
            brush = Brush.radialGradient(
                colors = colors,
                center = Offset(size.width / 2, size.width / -1.2f),
                radius = size.width,
            ),
        )
    }
