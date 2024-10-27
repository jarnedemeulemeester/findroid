package dev.jdtech.jellyfin.ui.components.player

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.Painter
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton

@Composable
fun VideoPlayerMediaButton(
    icon: Painter,
    onClick: () -> Unit = {},
) {
    val interactionSource = remember { MutableInteractionSource() }

    IconButton(onClick = onClick, interactionSource = interactionSource) {
        Icon(painter = icon, contentDescription = null)
    }
}
