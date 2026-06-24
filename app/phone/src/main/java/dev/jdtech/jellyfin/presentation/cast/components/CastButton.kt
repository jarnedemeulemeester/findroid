package dev.jdtech.jellyfin.presentation.cast.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jdtech.jellyfin.player.cast.Device
import dev.jdtech.jellyfin.player.cast.CastManager
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.core.R as CoreR

@Composable
fun CastButton(
    castManager: CastManager,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val devices by castManager.availableDevices.collectAsState()

    CastButtonContent(
        devices = devices,
        expanded = expanded,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
fun CastButtonContent(
    devices: List<Device>,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        expanded = expanded,
        icon = {
            Icon(
                painter = painterResource(CoreR.drawable.ic_cast),
                contentDescription = "Cast"
            )
        },
        text = {
            Text(
                text = when {
                    devices.isEmpty() -> "Connect TV"
                    devices.size == 1 -> "TV nearby"
                    else -> "${devices.size} TVs nearby"
                }
            )
        },
        modifier = modifier
    )

}

@Preview(showBackground = true)
@Composable
private fun CastButtonPreview() {
    FindroidTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Expanded states")
            // No devices
            CastButtonContent(
                devices = emptyList(),
                expanded = true,
                onClick = {}
            )
            // One device
            CastButtonContent(
                devices = listOf(Device("1", "Living Room TV")),
                expanded = true,
                onClick = {}
            )
            // Multiple devices
            CastButtonContent(
                devices = listOf(Device("1", "TV 1"), Device("2", "TV 2")),
                expanded = true,
                onClick = {}
            )

            Text("Collapsed state", modifier = Modifier.padding(top = 16.dp))
            CastButtonContent(
                devices = listOf(Device("1", "Living Room TV")),
                expanded = false,
                onClick = {}
            )
        }
    }
}