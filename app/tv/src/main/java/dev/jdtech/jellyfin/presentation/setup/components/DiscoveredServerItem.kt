package dev.jdtech.jellyfin.presentation.setup.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.core.presentation.dummy.dummyServer
import dev.jdtech.jellyfin.core.R as CoreR

@Composable
fun DiscoveredServerItem(
    name: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Column(
        modifier = modifier.width(80.dp),
    ) {
        Surface(
            onClick = onClick,
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
                    shape = RoundedCornerShape(16.dp),
                ),
            ),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
        ) {
            Icon(
                painter = painterResource(CoreR.drawable.ic_server),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.Center),
            )
        }
        Spacer(modifier = Modifier.height(MaterialTheme.spacings.small))
        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            maxLines = 1,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
@Preview
private fun DiscoveredServerItemPreview() {
    FindroidTheme {
        DiscoveredServerItem(dummyServer.name)
    }
}
