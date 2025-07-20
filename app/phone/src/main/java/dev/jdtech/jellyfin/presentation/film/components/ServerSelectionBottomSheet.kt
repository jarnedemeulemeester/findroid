package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jdtech.jellyfin.core.presentation.dummy.dummyServer
import dev.jdtech.jellyfin.core.presentation.dummy.dummyServerAddress
import dev.jdtech.jellyfin.models.ServerWithAddresses
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.setup.presentation.servers.ServersAction
import dev.jdtech.jellyfin.setup.presentation.servers.ServersEvent
import dev.jdtech.jellyfin.setup.presentation.servers.ServersState
import dev.jdtech.jellyfin.setup.presentation.servers.ServersViewModel
import dev.jdtech.jellyfin.utils.ObserveAsEvents

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerSelectionBottomSheet(
    currentServerId: String,
    onUpdate: () -> Unit,
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(),
    viewModel: ServersViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(true) {
        viewModel.loadServers()
    }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is ServersEvent.NavigateToUsers -> onUpdate()
            is ServersEvent.AddressChanged -> onUpdate()
        }
    }

    ServerSelectionBottomSheetLayout(
        currentServerId = currentServerId,
        state = state,
        onAction = { action ->
            viewModel.onAction(action)
        },
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServerSelectionBottomSheetLayout(
    currentServerId: String,
    state: ServersState,
    onAction: (action: ServersAction) -> Unit,
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(),
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
    ) {
        LazyColumn(
            contentPadding = PaddingValues(
                start = MaterialTheme.spacings.medium,
                end = MaterialTheme.spacings.medium,
                bottom = MaterialTheme.spacings.default,
            ),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
        ) {
            items(
                items = state.servers,
                key = {
                    it.server.id
                },
            ) { server ->
                ServerSelectionItem(
                    server = server,
                    selected = server.server.id == currentServerId,
                    onClick = {
                        onAction(ServersAction.OnServerClick(server.server.id))
                    },
                    onClickAddress = { addressId ->
                        onAction(ServersAction.OnAddressClick(addressId = addressId))
                    },
                    modifier = Modifier
                        .fillMaxWidth(),
                )
            }
            item(
                key = "manage",
            ) {
                OutlinedButton(
                    onClick = {},
                    modifier = Modifier
                        .fillMaxWidth(),
                ) {
                    Text(
                        text = "Manage servers",
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
private fun ServerSelectionBottomSheetPreview() {
    FindroidTheme {
        ServerSelectionBottomSheetLayout(
            currentServerId = "",
            state = ServersState(
                servers = listOf(
                    ServerWithAddresses(
                        server = dummyServer,
                        addresses = listOf(
                            dummyServerAddress,
                        ),
                        user = null,
                    ),
                ),
            ),
            onAction = {},
            onDismissRequest = {},
            sheetState = rememberStandardBottomSheetState(initialValue = SheetValue.Expanded),
        )
    }
}
