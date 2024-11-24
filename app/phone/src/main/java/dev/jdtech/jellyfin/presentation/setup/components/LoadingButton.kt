package dev.jdtech.jellyfin.presentation.setup.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme

@Composable
fun LoadingButton(
    text: String,
    onClick: () -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Box {
        if (isLoading) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.CenterStart)
                    .offset(x = 8.dp),
            )
        }
        Button(
            onClick = onClick,
            enabled = !isLoading,
            modifier = modifier,
        ) {
            Text(text = text)
        }
    }
}

@Composable
@Preview
private fun LoadingButtonPreview() {
    FindroidTheme {
        LoadingButton(
            text = "Connect",
            onClick = {},
            isLoading = true,
        )
    }
}
