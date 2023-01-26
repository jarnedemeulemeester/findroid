package dev.jdtech.jellyfin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.models.DiscoveredServer
import dev.jdtech.jellyfin.ui.theme.FindroidTheme
import dev.jdtech.jellyfin.ui.theme.Typography
import dev.jdtech.jellyfin.viewmodels.AddServerViewModel

@Composable
fun AddServerScreen(addServerViewModel: AddServerViewModel = viewModel()) {
    val uiState by addServerViewModel.uiState.collectAsState()
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Banner()
        AddServerForm(uiState = uiState) {
            addServerViewModel.checkServer(it)
        }
    }
}

@Composable
fun Banner() {
    Icon(
        painter = painterResource(id = R.drawable.ic_banner),
        contentDescription = null,
        tint = Color.Unspecified,
        modifier = Modifier.width(320.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddServerForm(uiState: AddServerViewModel.UiState, onSubmit: (String) -> Unit) {
    var text by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }
    val isError = uiState is AddServerViewModel.UiState.Error
    val isLoading = uiState is AddServerViewModel.UiState.Loading
    val context = LocalContext.current

    val discoveredServers = listOf(
        DiscoveredServer("e9179766-1da2-4cea-98a4-e4e51fa7fbd0", "server1", "server1.local"),
        DiscoveredServer("236c6ef9-c870-47fb-ab47-bc064a7f92af", "server2", "server2.local")
    )
    Column(Modifier.width(320.dp)) {
        Text(text = stringResource(id = R.string.add_server), style = Typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(discoveredServers) { _, discoveredServer ->
                DiscoveredServerComponent(discoveredServer = discoveredServer)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = text,
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_server),
                    contentDescription = null
                )
            },
            onValueChange = { text = it },
            label = { Text(text = stringResource(id = R.string.edit_text_server_address_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                autoCorrect = false,
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Go
            ),
            isError = isError,
            modifier = Modifier
                .fillMaxWidth()
        )
        Text(
            text = if (isError) (uiState as AddServerViewModel.UiState.Error).message.joinToString {
                it.asString(
                    context.resources
                )
            } else "",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box {
            Button(
                onClick = {
                    onSubmit(text.text)
                },
                enabled = uiState !is AddServerViewModel.UiState.Loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(id = R.string.button_connect))
            }
            if (isLoading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .width(48.dp)
                        .height(48.dp)
                        .padding(8.dp)
                )
            }
        }
    }
}

@Composable
fun DiscoveredServerComponent(discoveredServer: DiscoveredServer, onClick: (DiscoveredServer) -> Unit = {}) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(64.dp)
            .clip(MaterialTheme.shapes.small)
            .clickable { onClick(discoveredServer) }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.primary)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_server),
                contentDescription = null,
                modifier = Modifier.align(Alignment.Center),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = discoveredServer.name,
            style = Typography.bodyMedium,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1
        )
    }
}

@Preview
@Composable
fun DiscoveredServerComponentPreview() {
    DiscoveredServerComponent(discoveredServer = DiscoveredServer("e9179766-1da2-4cea-98a4-e4e51fa7fbd0", "server", "server.local"))
}

@Preview(showBackground = true, widthDp = 1200, heightDp = 720)
@Composable
fun AddServerScreenPreview() {
    FindroidTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            AddServerScreen()
        }
    }
}