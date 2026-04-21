package dev.jdtech.jellyfin.ui.components.player

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme

@Composable
fun VideoPlayerMediaTitle(title: String, subtitle: String?) {
    Column {
        Text(text = title, style = MaterialTheme.typography.headlineMedium, color = Color.White)
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = .75f),
            )
        }
    }
}

@Preview
@Composable
private fun VideoPlayerMediaTitlePreview() {
    FindroidTheme {
        VideoPlayerMediaTitle(title = "S1:E23 - Handler One", subtitle = "86 EIGHTY-SIX")
    }
}
