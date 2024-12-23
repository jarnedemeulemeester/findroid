package dev.jdtech.jellyfin.presentation.setup.addserver

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jdtech.jellyfin.core.presentation.dummy.dummyDiscoveredServer
import dev.jdtech.jellyfin.presentation.setup.components.DiscoveredServerItem
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.setup.R
import dev.jdtech.jellyfin.setup.presentation.addserver.AddServerAction
import dev.jdtech.jellyfin.setup.presentation.addserver.AddServerEvent
import dev.jdtech.jellyfin.setup.presentation.addserver.AddServerState
import dev.jdtech.jellyfin.setup.presentation.addserver.AddServerViewModel
import dev.jdtech.jellyfin.utils.ObserveAsEvents

@Composable
fun AddServerScreen(
    onSuccess: () -> Unit,
    viewModel: AddServerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(true) {
        viewModel.discoverServers()
    }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is AddServerEvent.Success -> onSuccess()
        }
    }

    AddServerScreenLayout(
        state = state,
        onAction = { action ->
            viewModel.onAction(action)
        },
    )
}

@Composable
private fun AddServerScreenLayout(
    state: AddServerState,
    onAction: (AddServerAction) -> Unit,
) {
    var serverAddress by rememberSaveable {
        mutableStateOf("")
    }
    val context = LocalContext.current

    val focusRequester = remember { FocusRequester() }
    val doConnect = { onAction(AddServerAction.OnConnectClick(serverAddress)) }

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
                text = stringResource(id = R.string.add_server),
                style = MaterialTheme.typography.displayMedium,
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacings.default))
            AnimatedVisibility(state.discoveredServers.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.default),
                    contentPadding = PaddingValues(horizontal = MaterialTheme.spacings.default),
                ) {
                    items(state.discoveredServers) { discoveredServer ->
                        DiscoveredServerItem(
                            name = discoveredServer.name,
                            onClick = {
                                serverAddress = discoveredServer.address
                                onAction(AddServerAction.OnConnectClick(discoveredServer.address))
                            },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(MaterialTheme.spacings.small))
            OutlinedTextField(
                value = serverAddress,
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = dev.jdtech.jellyfin.core.R.drawable.ic_server),
                        contentDescription = null,
                    )
                },
                onValueChange = { serverAddress = it },
                label = {
                    Text(
                        text = stringResource(id = R.string.edit_text_server_address_hint),
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Go,
                ),
                keyboardActions = KeyboardActions(
                    onGo = { doConnect() },
                ),
                isError = state.error != null,
                enabled = !state.isLoading,
                supportingText = {
                    if (state.error != null) {
                        Text(
                            text = state.error!!.joinToString {
                                it.asString(
                                    context.resources,
                                )
                            },
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                },
                modifier = Modifier
                    .width(360.dp)
                    .focusRequester(focusRequester),
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacings.medium))
            Box {
                Button(
                    onClick = { doConnect() },
                    enabled = !state.isLoading,
                    modifier = Modifier.width(360.dp),
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                color = LocalContentColor.current,
                                modifier = Modifier
                                    .size(24.dp)
                                    .align(Alignment.CenterStart),
                            )
                        }
                        Text(
                            text = stringResource(id = R.string.add_server_btn_connect),
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(true) {
        focusRequester.requestFocus()
    }
}

@Preview(device = "id:tv_1080p")
@Composable
private fun AddServerScreenLayoutPreview() {
    FindroidTheme {
        AddServerScreenLayout(
            state = AddServerState(),
            onAction = {},
        )
    }
}

@Preview(device = "id:tv_1080p")
@Composable
private fun AddServerScreenLayoutDiscoveredPreview() {
    FindroidTheme {
        AddServerScreenLayout(
            state = AddServerState(
                discoveredServers = listOf(dummyDiscoveredServer),
            ),
            onAction = {},
        )
    }
}
