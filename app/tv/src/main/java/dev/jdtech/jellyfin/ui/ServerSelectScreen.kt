package dev.jdtech.jellyfin.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.AddServerScreenDestination
import com.ramcosta.composedestinations.generated.destinations.UserSelectScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.jdtech.jellyfin.models.ServerAddress
import dev.jdtech.jellyfin.models.ServerWithAddresses
import dev.jdtech.jellyfin.setup.presentation.servers.ServersAction
import dev.jdtech.jellyfin.setup.presentation.servers.ServersEvent
import dev.jdtech.jellyfin.setup.presentation.servers.ServersState
import dev.jdtech.jellyfin.setup.presentation.servers.ServersViewModel
import dev.jdtech.jellyfin.ui.dummy.dummyDiscoveredServer
import dev.jdtech.jellyfin.ui.dummy.dummyServer
import dev.jdtech.jellyfin.ui.theme.FindroidTheme
import dev.jdtech.jellyfin.ui.theme.spacings
import dev.jdtech.jellyfin.utils.ObserveAsEvents
import java.util.UUID
import dev.jdtech.jellyfin.setup.R as SetupR

@Destination<RootGraph>
@Composable
fun ServerSelectScreen(
    navigator: DestinationsNavigator,
    viewModel: ServersViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(true) {
        viewModel.loadServers()
    }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is ServersEvent.NavigateToLogin -> {
                navigator.navigate(UserSelectScreenDestination)
            }
            is ServersEvent.NavigateToUsers -> {
                navigator.navigate(UserSelectScreenDestination)
            }
        }
    }

    ServerSelectScreenLayout(
        state = state,
        onAction = { action ->
            when (action) {
                is ServersAction.OnAddClick -> navigator.navigate(AddServerScreenDestination)
                else -> Unit
            }
            viewModel.onAction(action)
        },
    )
}

@Composable
private fun ServerSelectScreenLayout(
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
                    modifier = Modifier.focusRequester(focusRequester),
                ) {
                    items(state.servers) { server ->
                        ServerComponent(name = server.server.name, address = server.addresses.first().address) { onAction(ServersAction.OnServerClick(server.server.id)) }
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
                Text(text = stringResource(id = SetupR.string.add_server))
            }
        }
    }
}

@Preview(device = "id:tv_1080p")
@Composable
private fun ServerSelectScreenLayoutPreview() {
    FindroidTheme {
        ServerSelectScreenLayout(
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
private fun ServerSelectScreenLayoutPreviewNoServers() {
    FindroidTheme {
        ServerSelectScreenLayout(
            state = ServersState(),
            onAction = {},
        )
    }
}

@Composable
private fun ServerComponent(
    name: String,
    address: String,
    onClick: () -> Unit = {},
) {
    Surface(
        onClick = onClick,
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color(0xFF132026),
            focusedContainerColor = Color(0xFF132026),
        ),
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(16.dp)),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                BorderStroke(
                    4.dp,
                    Color.White,
                ),
                shape = RoundedCornerShape(16.dp),
            ),
        ),
        modifier = Modifier
            .width(270.dp)
            .height(115.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxHeight()
                    .align(Alignment.Center)
                    .padding(
                        vertical = MaterialTheme.spacings.default,
                        horizontal = MaterialTheme.spacings.medium,
                    ),
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                Text(
                    text = address,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFBDBDBD),
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
        }
    }
}

@Preview
@Composable
private fun ServerComponentPreview() {
    FindroidTheme {
        ServerComponent(
            name = dummyDiscoveredServer.name,
            address = dummyDiscoveredServer.address,
        )
    }
}
