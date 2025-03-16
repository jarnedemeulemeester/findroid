package dev.jdtech.jellyfin.presentation.setup.addserver

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jdtech.jellyfin.presentation.setup.components.DiscoveredServerItem
import dev.jdtech.jellyfin.presentation.setup.components.LoadingButton
import dev.jdtech.jellyfin.presentation.setup.components.RootLayout
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.setup.presentation.addserver.AddServerAction
import dev.jdtech.jellyfin.setup.presentation.addserver.AddServerEvent
import dev.jdtech.jellyfin.setup.presentation.addserver.AddServerState
import dev.jdtech.jellyfin.setup.presentation.addserver.AddServerViewModel
import dev.jdtech.jellyfin.utils.ObserveAsEvents
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.setup.R as SetupR

@Composable
fun AddServerScreen(
    onSuccess: () -> Unit,
    onBackClick: () -> Unit,
    viewModel: AddServerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

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
            when (action) {
                is AddServerAction.OnBackClick -> onBackClick()
                else -> Unit
            }
            viewModel.onAction(action)
        },
    )
}

@Composable
private fun AddServerScreenLayout(
    state: AddServerState,
    onAction: (AddServerAction) -> Unit,
) {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    val scrollState = rememberScrollState()

    var serverAddress by rememberSaveable {
        mutableStateOf("")
    }

    val doConnect = { onAction(AddServerAction.OnConnectClick(serverAddress)) }

    LaunchedEffect(true) {
        focusRequester.requestFocus()
    }

    RootLayout {
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 24.dp)
                .widthIn(max = 480.dp)
                .align(Alignment.Center)
                .verticalScroll(scrollState),
        ) {
            Image(
                painter = painterResource(id = CoreR.drawable.ic_banner),
                contentDescription = null,
                modifier = Modifier
                    .width(250.dp)
                    .align(Alignment.CenterHorizontally),
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(text = stringResource(SetupR.string.add_server), style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            AnimatedVisibility(state.discoveredServers.isNotEmpty()) {
                LazyRow {
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
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = serverAddress,
                leadingIcon = {
                    Icon(
                        painter = painterResource(CoreR.drawable.ic_server),
                        contentDescription = null,
                    )
                },
                onValueChange = { serverAddress = it },
                label = {
                    Text(
                        text = stringResource(SetupR.string.edit_text_server_address_hint),
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
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )
            LoadingButton(
                text = stringResource(SetupR.string.add_server_btn_connect),
                onClick = { doConnect() },
                isLoading = state.isLoading,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        IconButton(
            onClick = { onAction(AddServerAction.OnBackClick) },
            modifier = Modifier.padding(start = 8.dp),
        ) {
            Icon(painter = painterResource(CoreR.drawable.ic_arrow_left), contentDescription = null)
        }
    }
}

@PreviewScreenSizes
@Composable
private fun AddServerScreenLayoutPreview() {
    FindroidTheme {
        AddServerScreenLayout(
            state = AddServerState(),
            onAction = {},
        )
    }
}
