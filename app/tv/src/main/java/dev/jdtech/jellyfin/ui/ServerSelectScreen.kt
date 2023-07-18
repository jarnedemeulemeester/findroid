package dev.jdtech.jellyfin.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.jdtech.jellyfin.models.DiscoveredServer
import dev.jdtech.jellyfin.ui.destinations.AddServerScreenDestination
import dev.jdtech.jellyfin.ui.destinations.LoginScreenDestination
import dev.jdtech.jellyfin.ui.destinations.UserSelectScreenDestination
import dev.jdtech.jellyfin.viewmodels.ServerSelectViewModel
import dev.jdtech.jellyfin.core.R as CoreR

@OptIn(ExperimentalTvMaterial3Api::class)
@Destination
@Composable
fun ServerSelectScreen(
    navigator: DestinationsNavigator,
    serverSelectViewModel: ServerSelectViewModel = hiltViewModel(),
) {
    val delegatedUiState by serverSelectViewModel.uiState.collectAsState()
    val delegatedDiscoveredServersState by serverSelectViewModel.discoveredServersState.collectAsState()
    val navigateToLogin by serverSelectViewModel.navigateToMain.collectAsState(initial = false)
    if (navigateToLogin) {
        navigator.navigate(LoginScreenDestination)
    }
    var servers = emptyList<DiscoveredServer>()
    var discoveredServers = emptyList<DiscoveredServer>()

    when (val uiState = delegatedUiState) {
        is ServerSelectViewModel.UiState.Normal -> {
            servers =
                uiState.servers.map { DiscoveredServer(id = it.id, name = it.name, address = "") }
        }

        else -> Unit
    }
    when (val discoveredServersState = delegatedDiscoveredServersState) {
        is ServerSelectViewModel.DiscoveredServersState.Servers -> {
            discoveredServers = discoveredServersState.servers
        }

        else -> Unit
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(Color.Black, Color(0xFF001721)))),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
        ) {
            Text(
                text = stringResource(id = CoreR.string.select_server),
                style = MaterialTheme.typography.displayMedium,
            )
            if (discoveredServers.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(id = CoreR.drawable.ic_sparkles),
                        contentDescription = null,
                        tint = Color(0xFFBDBDBD),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = pluralStringResource(
                            id = CoreR.plurals.discovered_servers,
                            count = discoveredServers.count(),
                            discoveredServers.count(),
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFBDBDBD),
                    )
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            if (servers.isEmpty() && discoveredServers.isEmpty()) {
                Text(
                    text = stringResource(id = CoreR.string.no_servers_found),
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                TvLazyRow(
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp),
                ) {
                    items(servers) {
                        ServerComponent(it) { server ->
                            navigator.navigate(UserSelectScreenDestination(serverId = server.id))
                        }
                    }
                    items(discoveredServers) {
                        ServerComponent(it, discovered = true)
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            OutlinedButton(
                onClick = {
                    navigator.navigate(AddServerScreenDestination)
                },
            ) {
                Text(text = stringResource(id = CoreR.string.add_server))
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ServerComponent(
    server: DiscoveredServer,
    discovered: Boolean = false,
    onClick: (DiscoveredServer) -> Unit = {},
) {
    Surface(
        onClick = {
            onClick(server)
        },
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
            if (discovered) {
                Icon(
                    painter = painterResource(id = CoreR.drawable.ic_sparkles),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(start = 12.dp, top = 12.dp),
                )
            }
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxHeight()
                    .align(Alignment.Center)
                    .padding(vertical = 24.dp, horizontal = 16.dp),
            ) {
                Text(
                    text = server.name,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                Text(
                    text = server.address,
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
fun ServerPreview() {
    ServerComponent(
        DiscoveredServer(
            id = "",
            name = "Demo server",
            address = "https://demo.jellyfin.org/stable",
        ),
    )
}

@Preview
@Composable
fun ServerPreviewDiscovered() {
    ServerComponent(
        DiscoveredServer(
            id = "",
            name = "Demo server",
            address = "https://demo.jellyfin.org/stable",
        ),
        true,
    )
}
