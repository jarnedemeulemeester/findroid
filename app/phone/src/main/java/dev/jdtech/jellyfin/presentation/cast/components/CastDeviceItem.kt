package dev.jdtech.jellyfin.presentation.cast.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jdtech.jellyfin.player.cast.models.CastConnectionState
import dev.jdtech.jellyfin.player.cast.models.Device
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.core.R as CoreR

@Composable
fun CastDeviceItem(
    device: Device,
    connected: Boolean,
    connectionState: CastConnectionState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cornerPercent by animateIntAsState(
        targetValue = if (connected) 50 else 15,
        animationSpec = spring(
            dampingRatio = 0.4f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "shapeAnimation"
    )

    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(cornerPercent),
        colors = CardDefaults.cardColors(
            containerColor = if (connected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacings.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!connected) {
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(CoreR.drawable.ic_monitor),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Spacer(Modifier.width(MaterialTheme.spacings.medium))

            Text(
                text = device.name,
                style = MaterialTheme.typography.titleMedium,
                color = if (connected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            if (connected) {
                Box (
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (connectionState == CastConnectionState.CONNECTING) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(MaterialTheme.spacings.extraSmall),
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Icon(
                            painter = painterResource(CoreR.drawable.ic_monitor_off),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CastDeviceItemPreview() {
    var isConnected by remember { mutableStateOf(false) }

    FindroidTheme {
        CastDeviceItem(
            device = Device("1", "Living Room TV"),
            connected = isConnected,
            connectionState = if (isConnected) CastConnectionState.CONNECTED else CastConnectionState.DISCONNECTED,
            onClick = { isConnected = !isConnected },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CastDeviceItemConnectingPreview() {
    FindroidTheme {
        CastDeviceItem(
            device = Device("1", "Living Room TV"),
            connected = true,
            connectionState = CastConnectionState.CONNECTING,
            onClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CastDeviceItemConnectedPreview() {
    FindroidTheme {
        CastDeviceItem(
            device = Device("1", "Living Room TV"),
            connected = true,
            connectionState = CastConnectionState.CONNECTED,
            onClick = {},
        )
    }
}
