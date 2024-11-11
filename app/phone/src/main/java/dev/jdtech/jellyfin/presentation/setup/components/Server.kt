package dev.jdtech.jellyfin.presentation.setup.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jdtech.jellyfin.models.Server
import dev.jdtech.jellyfin.ui.theme.FindroidTheme
import dev.jdtech.jellyfin.core.R as CoreR

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ServerItem(server: Server, modifier: Modifier = Modifier, onClick: () -> Unit = {}, onLongClick: () -> Unit = {}) {
    OutlinedCard(
        modifier = modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 24.dp, top = 16.dp, end = 16.dp, bottom = 16.dp),
        ) {
            Icon(
                painter = painterResource(CoreR.drawable.ic_server),
                contentDescription = null,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
            Spacer(modifier = Modifier.width(24.dp))
            Column {
                Text(text = server.name, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
@Preview
fun ServerItemPreview() {
    FindroidTheme {
        ServerItem(
            server = Server(
                id = "",
                name = "Jellyfin Server",
                currentUserId = null,
                currentServerAddressId = null,
            ),
        )
    }
}
