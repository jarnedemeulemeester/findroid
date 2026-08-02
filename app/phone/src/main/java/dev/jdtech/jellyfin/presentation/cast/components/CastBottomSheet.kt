package dev.jdtech.jellyfin.presentation.cast.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.mediarouter.media.MediaRouter
import dev.jdtech.jellyfin.player.cast.models.CastConnectionState
import dev.jdtech.jellyfin.player.cast.models.Device
import dev.jdtech.jellyfin.player.cast.presentation.CastPlayerViewModel
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.presentation.utils.rememberSafePadding
import dev.jdtech.jellyfin.core.R as CoreR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CastBottomSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    viewModel: CastPlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val devices = uiState.availableDevices
    val connectedDevice = uiState.connectedDevice
    val connectionState = uiState.connectionState

    DisposableEffect(viewModel) {
        viewModel.sessionManager.updateDiscovery(MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN)
        onDispose {
            viewModel.sessionManager.updateDiscovery()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = null,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        CastBottomSheetLayout(
            devices = devices,
            connectedDevice = connectedDevice,
            connectionState = connectionState,
            onDeviceSelected = {
                viewModel.sessionManager.connect(it)
            },
            onDisconnect = {
                viewModel.sessionManager.disconnect()
            }
        )
    }
}

@Composable
fun CastBottomSheetLayout(
    devices: List<Device>,
    connectedDevice: Device?,
    connectionState: CastConnectionState,
    onDeviceSelected: (Device) -> Unit,
    onDisconnect: () -> Unit,
) {
    val safePadding = rememberSafePadding()

    val paddingBottom = safePadding.bottom + MaterialTheme.spacings.default

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                bottom = paddingBottom
            ),
    ) {
        Text(
            text = if (devices.isEmpty()) {
                stringResource(CoreR.string.cast_searching)
            } else {
                stringResource(CoreR.string.cast_select_device)
            },
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(
                horizontal = 24.dp,
                vertical = MaterialTheme.spacings.medium
            )
        )

        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )

        Spacer(Modifier.height(MaterialTheme.spacings.medium))

        if (devices.isNotEmpty()) {
            Text(
                text = stringResource(CoreR.string.cast_available_devices).uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(horizontal = 24.dp),
            )

            Spacer(Modifier.height(MaterialTheme.spacings.medium))

            LazyColumn(
                modifier = Modifier.weight(1f, fill = false),
                contentPadding = PaddingValues(horizontal = MaterialTheme.spacings.medium),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
            ) {
                items(items = devices, key = { it.id }) { device ->
                    CastDeviceItem(
                        device = device,
                        connected = device.id == connectedDevice?.id,
                        connectionState = connectionState,
                        onClick = {
                            if (device.id == connectedDevice?.id) {
                                onDisconnect()
                            } else {
                                onDeviceSelected(device)
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.height(MaterialTheme.spacings.medium))
        }

        Text(
            text = stringResource(CoreR.string.cast_wifi_instruction),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun CastBottomSheetSearchingPreview() {
    FindroidTheme {
        CastBottomSheetLayout(
            devices = emptyList(),
            connectedDevice = null,
            connectionState = CastConnectionState.DISCONNECTED,
            onDeviceSelected = {},
            onDisconnect = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun CastBottomSheetPreview() {
    val devices = listOf(
        Device("1", "Living Room TV"),
        Device("2", "Bedroom")
    )
    FindroidTheme {
        CastBottomSheetLayout(
            devices = devices,
            connectedDevice = null,
            connectionState = CastConnectionState.DISCONNECTED,
            onDeviceSelected = {},
            onDisconnect = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun CastBottomSheetConnectingPreview() {
    val devices = listOf(
        Device("1", "Living Room TV"),
        Device("2", "Bedroom")
    )
    FindroidTheme {
        CastBottomSheetLayout(
            devices = devices,
            connectedDevice = devices[0],
            connectionState = CastConnectionState.CONNECTING,
            onDeviceSelected = {},
            onDisconnect = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun CastBottomSheetConnectedPreview() {
    val devices = listOf(
        Device("1", "Living Room TV"),
        Device("2", "Bedroom")
    )
    FindroidTheme {
        CastBottomSheetLayout(
            devices = devices,
            connectedDevice = devices[0],
            connectionState = CastConnectionState.CONNECTED,
            onDeviceSelected = {},
            onDisconnect = {}
        )
    }
}