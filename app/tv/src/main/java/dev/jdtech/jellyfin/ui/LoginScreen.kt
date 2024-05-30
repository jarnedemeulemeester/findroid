package dev.jdtech.jellyfin.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.navigation.popUpTo
import dev.jdtech.jellyfin.NavGraphs
import dev.jdtech.jellyfin.destinations.MainScreenDestination
import dev.jdtech.jellyfin.models.UiText
import dev.jdtech.jellyfin.ui.theme.FindroidTheme
import dev.jdtech.jellyfin.ui.theme.spacings
import dev.jdtech.jellyfin.utils.ObserveAsEvents
import dev.jdtech.jellyfin.viewmodels.LoginEvent
import dev.jdtech.jellyfin.viewmodels.LoginViewModel
import dev.jdtech.jellyfin.core.R as CoreR

@Destination
@Composable
fun LoginScreen(
    navigator: DestinationsNavigator,
    loginViewModel: LoginViewModel = hiltViewModel(),
) {
    val delegatedUiState by loginViewModel.uiState.collectAsState()
    val delegatedQuickConnectUiState by loginViewModel.quickConnectUiState.collectAsState(
        initial = LoginViewModel.QuickConnectUiState.Disabled,
    )

    ObserveAsEvents(loginViewModel.eventsChannelFlow) { event ->
        when (event) {
            is LoginEvent.NavigateToHome -> {
                navigator.navigate(MainScreenDestination) {
                    popUpTo(NavGraphs.root) {
                        inclusive = true
                    }
                }
            }
        }
    }

    LoginScreenLayout(
        uiState = delegatedUiState,
        quickConnectUiState = delegatedQuickConnectUiState,
        onLoginClick = { username, password ->
            loginViewModel.login(username, password)
        },
        onQuickConnectClick = {
            loginViewModel.useQuickConnect()
        },
    )
}

@Composable
private fun LoginScreenLayout(
    uiState: LoginViewModel.UiState,
    quickConnectUiState: LoginViewModel.QuickConnectUiState,
    onLoginClick: (String, String) -> Unit,
    onQuickConnectClick: () -> Unit,
) {
    var username by rememberSaveable {
        mutableStateOf("")
    }
    var password by rememberSaveable {
        mutableStateOf("")
    }

    var quickConnectValue = stringResource(id = CoreR.string.quick_connect)

    when (quickConnectUiState) {
        is LoginViewModel.QuickConnectUiState.Waiting -> {
            quickConnectValue = quickConnectUiState.code
        }
        else -> Unit
    }

    var disclaimer: String? by remember {
        mutableStateOf(null)
    }

    if (uiState is LoginViewModel.UiState.Normal) {
        disclaimer = uiState.disclaimer
    }

    val isError = uiState is LoginViewModel.UiState.Error
    val isLoading = uiState is LoginViewModel.UiState.Loading

    val quickConnectEnabled = quickConnectUiState !is LoginViewModel.QuickConnectUiState.Disabled
    val isWaiting = quickConnectUiState is LoginViewModel.QuickConnectUiState.Waiting

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
                text = stringResource(id = CoreR.string.login),
                style = MaterialTheme.typography.displayMedium,
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacings.large))
            OutlinedTextField(
                value = username,
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = CoreR.drawable.ic_user),
                        contentDescription = null,
                    )
                },
                onValueChange = { username = it },
                label = { Text(text = stringResource(id = CoreR.string.edit_text_username_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    autoCorrect = false,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                ),
                isError = isError,
                enabled = !isLoading,
                modifier = Modifier
                    .width(360.dp)
                    .focusRequester(focusRequester),
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacings.medium))
            OutlinedTextField(
                value = password,
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = CoreR.drawable.ic_lock),
                        contentDescription = null,
                    )
                },
                onValueChange = { password = it },
                label = { Text(text = stringResource(id = CoreR.string.edit_text_password_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    autoCorrect = false,
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Go,
                ),
                visualTransformation = PasswordVisualTransformation(),
                isError = isError,
                enabled = !isLoading,
                supportingText = {
                    if (isError) {
                        Text(
                            text = (uiState as LoginViewModel.UiState.Error).message.asString(),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                },
                modifier = Modifier
                    .width(360.dp),
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacings.default))
            Box {
                Button(
                    onClick = {
                        onLoginClick(username, password)
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
                            text = stringResource(id = CoreR.string.button_login),
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                }
            }
            if (quickConnectEnabled) {
                Spacer(modifier = Modifier.height(MaterialTheme.spacings.medium))
                Box {
                    OutlinedButton(
                        onClick = {
                            onQuickConnectClick()
                        },
                        modifier = Modifier.width(360.dp),
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            if (isWaiting) {
                                CircularProgressIndicator(
                                    color = LocalContentColor.current,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .align(Alignment.CenterStart),
                                )
                            }
                            Text(
                                text = quickConnectValue,
                                modifier = Modifier.align(Alignment.Center),
                            )
                        }
                    }
                }
            }
            Text(
                text = disclaimer ?: "",
                modifier = Modifier.padding(MaterialTheme.spacings.default),
            )
        }
    }

    LaunchedEffect(true) {
        focusRequester.requestFocus()
    }
}

@Preview(device = "id:tv_1080p")
@Composable
private fun LoginScreenLayoutPreview() {
    FindroidTheme {
        LoginScreenLayout(
            uiState = LoginViewModel.UiState.Normal(),
            quickConnectUiState = LoginViewModel.QuickConnectUiState.Normal,
            onLoginClick = { _, _ -> },
            onQuickConnectClick = {},
        )
    }
}

@Preview(device = "id:tv_1080p")
@Composable
private fun LoginScreenLayoutPreviewError() {
    FindroidTheme {
        LoginScreenLayout(
            uiState = LoginViewModel.UiState.Error(UiText.DynamicString("Invalid username or password")),
            quickConnectUiState = LoginViewModel.QuickConnectUiState.Normal,
            onLoginClick = { _, _ -> },
            onQuickConnectClick = {},
        )
    }
}
