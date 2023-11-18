package dev.jdtech.jellyfin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.jdtech.jellyfin.destinations.LoginScreenDestination
import dev.jdtech.jellyfin.ui.theme.FindroidTheme
import dev.jdtech.jellyfin.ui.theme.spacings
import dev.jdtech.jellyfin.utils.ObserveAsEvents
import dev.jdtech.jellyfin.viewmodels.AddServerEvent
import dev.jdtech.jellyfin.viewmodels.AddServerViewModel
import dev.jdtech.jellyfin.core.R as CoreR

@Destination
@Composable
fun AddServerScreen(
    navigator: DestinationsNavigator,
    addServerViewModel: AddServerViewModel = hiltViewModel(),
) {
    val uiState by addServerViewModel.uiState.collectAsState()

    ObserveAsEvents(addServerViewModel.eventsChannelFlow) { event ->
        when (event) {
            is AddServerEvent.NavigateToLogin -> {
                navigator.navigate(LoginScreenDestination)
            }
        }
    }

    AddServerScreenLayout(
        uiState = uiState,
        onConnectClick = { serverAddress ->
            addServerViewModel.checkServer(serverAddress)
        },
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AddServerScreenLayout(
    uiState: AddServerViewModel.UiState,
    onConnectClick: (String) -> Unit,
) {
    var serverAddress by rememberSaveable {
        mutableStateOf("")
    }
    val isError = uiState is AddServerViewModel.UiState.Error
    val isLoading = uiState is AddServerViewModel.UiState.Loading
    val context = LocalContext.current

    val focusRequester = remember { FocusRequester() }

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
                text = stringResource(id = CoreR.string.add_server),
                style = MaterialTheme.typography.displayMedium,
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacings.large))
            OutlinedTextField(
                value = serverAddress,
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = CoreR.drawable.ic_server),
                        contentDescription = null,
                    )
                },
                onValueChange = { serverAddress = it },
                label = {
                    Text(
                        text = stringResource(id = CoreR.string.edit_text_server_address_hint),
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    autoCorrect = false,
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Go,
                ),
                isError = isError,
                enabled = !isLoading,
                supportingText = {
                    if (isError) {
                        Text(
                            text = (uiState as AddServerViewModel.UiState.Error).message.joinToString {
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
                    onClick = {
                        onConnectClick(serverAddress)
                    },
                    enabled = !isLoading,
                    modifier = Modifier.width(360.dp),
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = LocalContentColor.current,
                                modifier = Modifier
                                    .size(24.dp)
                                    .align(Alignment.CenterStart),
                            )
                        }
                        Text(
                            text = stringResource(id = CoreR.string.button_connect),
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

@OptIn(ExperimentalTvMaterial3Api::class)
@Preview(widthDp = 960, heightDp = 540)
@Composable
private fun AddServerScreenLayoutPreview() {
    FindroidTheme {
        Surface {
            AddServerScreenLayout(
                uiState = AddServerViewModel.UiState.Normal,
                onConnectClick = {},
            )
        }
    }
}
