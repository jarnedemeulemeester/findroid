package dev.jdtech.jellyfin.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import dev.jdtech.jellyfin.core.R
import dev.jdtech.jellyfin.ui.theme.FindroidTheme

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ProfileButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = {
            onClick()
        },
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color.Transparent,
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(1.dp, Color.White), shape = CircleShape),
            focusedBorder = Border(BorderStroke(4.dp, Color.White), shape = CircleShape),
        ),
        shape = ClickableSurfaceDefaults.shape(
            shape = CircleShape,
            focusedShape = CircleShape,
        ),
        modifier = modifier
            .width(32.dp)
            .aspectRatio(1f),
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_user),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .width(16.dp)
                .height(16.dp)
                .align(Alignment.Center),
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Preview
@Composable
private fun ProfileButtonPreview() {
    FindroidTheme {
        Surface {
            ProfileButton(
                onClick = {}
            )
        }
    }
}