package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jdtech.jellyfin.core.presentation.dummy.dummyServer
import dev.jdtech.jellyfin.core.presentation.dummy.dummyServerAddress
import dev.jdtech.jellyfin.models.ServerWithAddresses
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.core.R as CoreR

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ServerSelectionItem(
    server: ServerWithAddresses,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .padding(MaterialTheme.spacings.medium),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent),
            ) {
                Icon(
                    painter = painterResource(CoreR.drawable.ic_server),
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.Center),
                    tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else LocalContentColor.current,
                )
            }
            Spacer(modifier = Modifier.width(MaterialTheme.spacings.medium))
            Column {
                Text(
                    text = server.server.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = server.addresses.first().address,
                    style = MaterialTheme.typography.bodyMedium,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
@Preview
private fun ServerSelectionItemPreview() {
    FindroidTheme {
        ServerSelectionItem(
            server = ServerWithAddresses(
                server = dummyServer,
                addresses = listOf(
                    dummyServerAddress,
                ),
                user = null,
            ),
            selected = false,
        )
    }
}

@Composable
@Preview
private fun ServerSelectionItemSelectedPreview() {
    FindroidTheme {
        ServerSelectionItem(
            server = ServerWithAddresses(
                server = dummyServer,
                addresses = listOf(
                    dummyServerAddress,
                ),
                user = null,
            ),
            selected = true,
        )
    }
}
