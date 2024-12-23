package dev.jdtech.jellyfin.presentation.setup.servers

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import dev.jdtech.jellyfin.models.ServerAddress
import dev.jdtech.jellyfin.models.ServerWithAddresses
import dev.jdtech.jellyfin.presentation.setup.components.ServerItem
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.setup.R
import dev.jdtech.jellyfin.setup.presentation.servers.ServersAction
import dev.jdtech.jellyfin.setup.presentation.servers.ServersEvent
import dev.jdtech.jellyfin.setup.presentation.servers.ServersState
import dev.jdtech.jellyfin.setup.presentation.servers.ServersViewModel
import dev.jdtech.jellyfin.core.presentation.dummy.dummyDiscoveredServer
import dev.jdtech.jellyfin.core.presentation.dummy.dummyServer
import dev.jdtech.jellyfin.utils.ObserveAsEvents
import java.util.UUID

@Composable
fun ServersScreen(
    navigateToLogin: () -> Unit,
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
            is ServersEvent.NavigateToLogin -> navigateToLogin()
            is ServersEvent.NavigateToUsers -> navigateToUsers()
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
    val focusRequester = remember { FocusRequester() }

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
                text = stringResource(id = R.string.servers),
                style = MaterialTheme.typography.displayMedium,
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacings.large))
            if (state.servers.isEmpty()) {
                Text(
                    text = stringResource(id = R.string.servers_no_servers),
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.large),
                    contentPadding = PaddingValues(horizontal = MaterialTheme.spacings.default),
                    modifier = Modifier.focusRequester(focusRequester),
                ) {
                    items(state.servers) { server ->
                        ServerItem(
                            name = server.server.name,
                            address = server.addresses.first().address,
                        ) { onAction(ServersAction.OnServerClick(server.server.id)) }
                    }
                }

                LaunchedEffect(true) {
                    focusRequester.requestFocus()
                }
            }
            Spacer(modifier = Modifier.height(MaterialTheme.spacings.large))
            OutlinedButton(
                onClick = { onAction(ServersAction.OnAddClick) },
            ) {
                Text(text = stringResource(id = R.string.add_server))
            }
        }
    }
}

@Preview(device = "id:tv_1080p")
@Composable
private fun ServersScreenLayoutPreview() {
    FindroidTheme {
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
    FindroidTheme {
        ServersScreenLayout(
            state = ServersState(),
            onAction = {},
        )
    }
}
