package dev.jdtech.jellyfin.presentation.cast.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jdtech.jellyfin.player.cast.models.Device
import dev.jdtech.jellyfin.player.cast.presentation.CastPlayerViewModel
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.presentation.utils.rememberSafePadding
import dev.jdtech.jellyfin.core.R as CoreR

@Composable
fun CastButton(
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    handleBottomInsets: Boolean = true,
    viewModel: CastPlayerViewModel = hiltViewModel(LocalContext.current as ViewModelStoreOwner)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val devices = uiState.availableDevices

    CastButtonContent(
        devices = devices,
        expanded = expanded,
        onClick = onClick,
        modifier = modifier,
        handleBottomInsets = handleBottomInsets
    )
}

@Composable
fun CastButtonContent(
    devices: List<Device>,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    handleBottomInsets: Boolean = true
) {
    val safePadding = rememberSafePadding(handleBottomInsets = handleBottomInsets)

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
                text = if (devices.isEmpty()) {
                    stringResource(CoreR.string.cast_connect_tv)
                } else {
                    pluralStringResource(CoreR.plurals.cast_tvs_nearby, devices.size, devices.size)
                }
            )
        },
        modifier = modifier.padding(
            bottom = safePadding.bottom + MaterialTheme.spacings.medium,
            end = safePadding.end + MaterialTheme.spacings.medium
        )
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
