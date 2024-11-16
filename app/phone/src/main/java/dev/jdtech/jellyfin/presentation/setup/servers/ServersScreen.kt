package dev.jdtech.jellyfin.presentation.setup.servers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jdtech.jellyfin.models.Server
import dev.jdtech.jellyfin.models.ServerAddress
import dev.jdtech.jellyfin.models.ServerWithAddresses
import dev.jdtech.jellyfin.presentation.setup.components.RootLayout
import dev.jdtech.jellyfin.presentation.setup.components.ServerItem
import dev.jdtech.jellyfin.setup.presentation.servers.ServersAction
import dev.jdtech.jellyfin.setup.presentation.servers.ServersEvent
import dev.jdtech.jellyfin.setup.presentation.servers.ServersState
import dev.jdtech.jellyfin.setup.presentation.servers.ServersViewModel
import dev.jdtech.jellyfin.ui.theme.FindroidTheme
import dev.jdtech.jellyfin.utils.ObserveAsEvents
import java.util.UUID
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.setup.R as SetupR

@Composable
fun ServersScreen(
    navigateToUsers: () -> Unit,
    navigateToLogin: () -> Unit,
    onAddClick: () -> Unit,
    onBackClick: () -> Unit,
    viewModel: ServersViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(true) {
        viewModel.loadServers()
    }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is ServersEvent.NavigateToUsers -> navigateToUsers()
            is ServersEvent.NavigateToLogin -> navigateToLogin()
            else -> Unit
        }
    }

    ServersScreenLayout(
        state = state,
        onAction = { action ->
            when (action) {
                is ServersAction.OnAddClick -> onAddClick()
                is ServersAction.OnBackClick -> onBackClick()
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

    RootLayout {
        IconButton(
            onClick = { onAction(ServersAction.OnBackClick) },
            modifier = Modifier.padding(start = 8.dp),
        ) {
            Icon(painter = painterResource(CoreR.drawable.ic_arrow_left), contentDescription = null)
        }
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .widthIn(max = 480.dp)
                .align(Alignment.Center),
        ) {
            Spacer(modifier = Modifier.weight(0.2f))
            Icon(
                painter = painterResource(id = CoreR.drawable.ic_banner),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier
                    .width(250.dp)
                    .align(Alignment.CenterHorizontally),
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(text = stringResource(SetupR.string.servers), style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(32.dp))
            if (state.servers.isEmpty()) {
                Text(
                    text = stringResource(SetupR.string.servers_no_servers),
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    items(state.servers) { server ->
                        ServerItem(
                            name = server.server.name,
                            address = server.addresses.first().address,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                onAction(
                                    ServersAction.OnServerClick(serverId = server.server.id),
                                )
                            },
                            onLongClick = {
                                selectedServer = server.server
                                openDeleteDialog = true
                            },
                        )
                    }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { onAction(ServersAction.OnAddClick) },
            icon = { Icon(painterResource(CoreR.drawable.ic_plus), contentDescription = null) },
            text = { Text(text = stringResource(SetupR.string.servers_btn_add_server)) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
        )
    }

    if (openDeleteDialog && selectedServer != null) {
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
                ) {
                    Text(text = stringResource(SetupR.string.cancel))
                }
            },
        )
    }
}

@PreviewScreenSizes
@Preview
@Composable
private fun ServersScreenLayoutEmptyPreview() {
    FindroidTheme {
        ServersScreenLayout(
            state = ServersState(
                servers = listOf(
                    ServerWithAddresses(
                        server = Server(
                            id = "",
                            name = "Jellyfin Server",
                            currentServerAddressId = null,
                            currentUserId = null,
                        ),
                        addresses = listOf(
                            ServerAddress(
                                id = UUID.randomUUID(),
                                address = "http://192.168.0.10:8096",
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
