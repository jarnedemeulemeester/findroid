package dev.jdtech.jellyfin.presentation.setup.discoverserver

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jdtech.jellyfin.models.DiscoveredServer
import dev.jdtech.jellyfin.presentation.setup.components.DiscoveredServerItem
import dev.jdtech.jellyfin.presentation.setup.components.RootLayout
import dev.jdtech.jellyfin.setup.presentation.discoverserver.DiscoverServerAction
import dev.jdtech.jellyfin.setup.presentation.discoverserver.DiscoverServerEvent
import dev.jdtech.jellyfin.setup.presentation.discoverserver.DiscoverServerState
import dev.jdtech.jellyfin.setup.presentation.discoverserver.DiscoverServerViewModel
import dev.jdtech.jellyfin.ui.theme.FindroidTheme
import dev.jdtech.jellyfin.utils.ObserveAsEvents
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.setup.R as SetupR

@Composable
fun DiscoverServerScreen(
    onSuccess: () -> Unit,
    onManualClick: () -> Unit,
    onBackClick: () -> Unit,
    viewModel: DiscoverServerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(true) {
        viewModel.discoverServers()
    }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is DiscoverServerEvent.Success -> {
                onSuccess()
            }
        }
    }

    DiscoverServerScreenLayout(
        state = state,
        onAction = { action ->
            when (action) {
                is DiscoverServerAction.OnManualClick -> onManualClick()
                is DiscoverServerAction.OnBackClick -> onBackClick()
                else -> Unit
            }
            viewModel.onAction(action)
        },
    )
}

@Composable
private fun DiscoverServerScreenLayout(
    state: DiscoverServerState,
    onAction: (DiscoverServerAction) -> Unit,
) {
    RootLayout {
        IconButton(
            onClick = { onAction(DiscoverServerAction.OnBackClick) },
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
            Text(text = stringResource(SetupR.string.discover_server_title), style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(32.dp))
            Box(
                modifier = Modifier.weight(1f),
            ) {
                when {
                    state.servers.isEmpty() && state.isLoading -> {
                        CircularProgressIndicator()
                    }
                    state.servers.isEmpty() && !state.isLoading -> {
                        Text(text = stringResource(SetupR.string.discover_server_no_servers_found), style = MaterialTheme.typography.bodyMedium)
                    }
                    else -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(state.servers) { server ->
                                DiscoveredServerItem(discoveredServer = server, modifier = Modifier.fillMaxWidth(), onClick = {
                                    onAction(
                                        DiscoverServerAction.OnServerClick(address = server.address),
                                    )
                                })
                            }
                        }
                    }
                }
            }
            Text(text = stringResource(SetupR.string.discover_server_manual_add), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            OutlinedButton(
                onClick = { onAction(DiscoverServerAction.OnManualClick) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(SetupR.string.discover_server_btn_manual_add))
            }
        }
    }
}

@PreviewScreenSizes
@Preview
@Composable
private fun DiscoverServerScreenLayoutEmptyPreview() {
    FindroidTheme {
        DiscoverServerScreenLayout(
            state = DiscoverServerState(),
            onAction = {},
        )
    }
}

@PreviewScreenSizes
@Preview
@Composable
private fun DiscoverServerScreenLayoutServersPreview() {
    FindroidTheme {
        DiscoverServerScreenLayout(
            state = DiscoverServerState(
                servers = listOf(
                    DiscoveredServer(
                        id = "",
                        name = "Jellyfin Server",
                        address = "http://192.168.0.10:8096",
                    ),
                    DiscoveredServer(
                        id = "",
                        name = "Jellyfin Server",
                        address = "http://192.168.0.10:8096",
                    ),
                ),
            ),
            onAction = {},
        )
    }
}
