package dev.jdtech.jellyfin.ui.components.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun VideoPlayerMediaButton(
    icon: Painter,
    onClick: () -> Unit = {},
) {
    IconButton(onClick = onClick) {
        Icon(painter = icon, contentDescription = null)
    }
}
