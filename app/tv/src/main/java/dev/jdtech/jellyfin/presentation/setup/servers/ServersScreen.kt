package dev.jdtech.jellyfin.presentation.setup.servers

import android.view.KeyEvent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import dev.jdtech.jellyfin.core.presentation.dummy.dummyDiscoveredServer
import dev.jdtech.jellyfin.core.presentation.dummy.dummyServer
import dev.jdtech.jellyfin.models.Server
import dev.jdtech.jellyfin.models.ServerAddress
import dev.jdtech.jellyfin.models.ServerWithAddresses
import dev.jdtech.jellyfin.presentation.setup.components.ServerItem
import dev.jdtech.jellyfin.presentation.theme.JellyCastTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.setup.presentation.servers.ServersAction
import dev.jdtech.jellyfin.setup.presentation.servers.ServersEvent
import dev.jdtech.jellyfin.setup.presentation.servers.ServersState
import dev.jdtech.jellyfin.setup.presentation.servers.ServersViewModel
import dev.jdtech.jellyfin.utils.ObserveAsEvents
import java.util.UUID
import dev.jdtech.jellyfin.setup.R as SetupR

@Composable
fun ServersScreen(
    navigateToUsers: () -> Unit,
    onAddClick: () -> Unit,
    viewModel: ServersViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(true) {
        viewModel.loadServers()
    }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is ServersEvent.ServerChanged -> navigateToUsers()
            else -> Unit
        }
    }

    ServersScreenLayout(
        state = state,
        onAction = { action ->
            when (action) {
                is ServersAction.OnAddClick -> onAddClick()
                else -> Unit
            }
            viewModel.onAction(action)
        },
    )
}

@Composable
private fun ServersScreenLayout(
    state: ServersState,
    onAction: (ServersAction) -> Unit,
) {
    var openDeleteDialog by remember { mutableStateOf(false) }
    var selectedServer by remember { mutableStateOf<Server?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
        ) {
            Text(
                text = stringResource(id = SetupR.string.servers),
                style = MaterialTheme.typography.displayMedium,
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacings.large))
            if (state.servers.isEmpty()) {
                Text(
                    text = stringResource(id = SetupR.string.servers_no_servers),
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.large),
                    contentPadding = PaddingValues(horizontal = MaterialTheme.spacings.default),
                ) {
                    items(state.servers) { server ->
                        ServerItem(
                            name = server.server.name,
                            address = server.addresses.first().address,
                            onClick = {
                                onAction(ServersAction.OnServerClick(server.server.id))
                            },
                            onLongClick = {
                                selectedServer = server.server
                                openDeleteDialog = true
                            },
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(MaterialTheme.spacings.large))
            OutlinedButton(
                onClick = { onAction(ServersAction.OnAddClick) },
            ) {
                Text(text = stringResource(id = SetupR.string.add_server))
            }
        }
    }

    if (openDeleteDialog && selectedServer != null) {
        var firstInteraction by remember { mutableStateOf(true) }
        AlertDialog(
            title = {
                Text(text = stringResource(SetupR.string.remove_server_dialog))
            },
            text = {
                Text(text = stringResource(SetupR.string.remove_server_dialog_text, selectedServer!!.name))
            },
            onDismissRequest = {
                openDeleteDialog = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        openDeleteDialog = false
                        onAction(ServersAction.DeleteServer(selectedServer!!.id))
                    },
                ) {
                    Text(text = stringResource(SetupR.string.confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        openDeleteDialog = false
                    },
                    modifier = Modifier.onPreviewKeyEvent { event ->
                        // Long press on server would trigger the cancel button. This fixes that by capturing the first up event.
                        when (event.nativeKeyEvent.keyCode) {
                            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                                if (firstInteraction) {
                                    if (event.nativeKeyEvent.action == KeyEvent.ACTION_UP) {
                                        firstInteraction = false
                                    }
                                    return@onPreviewKeyEvent true
                                }
                            }
                        }
                        false
                    },
                ) {
                    Text(text = stringResource(SetupR.string.cancel))
                }
            },
        )
    }
}

@Preview(device = "id:tv_1080p")
@Composable
private fun ServersScreenLayoutPreview() {
    JellyCastTheme {
        ServersScreenLayout(
            state = ServersState(
                servers = listOf(
                    ServerWithAddresses(
                        server = dummyServer,
                        addresses = listOf(
                            ServerAddress(
                                id = UUID.randomUUID(),
                                address = dummyDiscoveredServer.address,
                                serverId = "",
                            ),
                        ),
                        user = null,
                    ),
                ),
            ),
            onAction = {},
        )
    }
}

@Preview(device = "id:tv_1080p")
@Composable
private fun ServersScreenLayoutPreviewNoServers() {
    JellyCastTheme {
        ServersScreenLayout(
            state = ServersState(),
            onAction = {},
        )
    }
}
