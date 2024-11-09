package dev.jdtech.jellyfin.presentation.setup.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jdtech.jellyfin.models.DiscoveredServer
import dev.jdtech.jellyfin.ui.theme.FindroidTheme
import dev.jdtech.jellyfin.core.R as CoreR

@Composable
fun DiscoveredServerItem(discoveredServer: DiscoveredServer, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(start = 24.dp, top = 16.dp, end = 16.dp, bottom = 16.dp),
        ) {
            Icon(
                painter = painterResource(CoreR.drawable.ic_server),
                contentDescription = null,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
            Spacer(modifier = Modifier.width(24.dp))
            Column {
                Text(text = discoveredServer.name, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = discoveredServer.address, style = MaterialTheme.typography.bodyMedium, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
@Preview
fun DiscoveredServerItemPreview() {
    FindroidTheme {
        DiscoveredServerItem(
            discoveredServer = DiscoveredServer(
                id = "",
                name = "Jellyfin Server",
                address = "http://192.168.0.10:8096",
            ),
        )
    }
}
