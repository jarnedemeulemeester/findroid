package dev.jdtech.jellyfin.presentation.setup.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.jdtech.jellyfin.core.presentation.dummy.dummyDiscoveredServer
import dev.jdtech.jellyfin.presentation.theme.JellyCastTheme
import dev.jdtech.jellyfin.presentation.theme.spacings

@Composable
fun ServerItem(
    name: String,
    address: String,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
) {
    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color(0xFF132026),
            focusedContainerColor = Color(0xFF132026),
        ),
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(16.dp)),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                BorderStroke(
                    4.dp,
                    Color.White,
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            ),
        ),
        modifier = Modifier
            .width(270.dp)
            .height(115.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxHeight()
                    .align(Alignment.Center)
                    .padding(
                        vertical = MaterialTheme.spacings.default,
                        horizontal = MaterialTheme.spacings.medium,
                    ),
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                Text(
                    text = address,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFBDBDBD),
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
        }
    }
}

@Preview
@Composable
private fun ServerItemPreview() {
    JellyCastTheme {
        ServerItem(
            name = dummyDiscoveredServer.name,
            address = dummyDiscoveredServer.address,
        )
    }
}
