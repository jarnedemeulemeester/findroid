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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.ui.destinations.HomeScreenDestination
import dev.jdtech.jellyfin.viewmodels.LoginViewModel

@OptIn(ExperimentalTvMaterial3Api::class)
@Destination
@Composable
fun LoginScreen(
    navigator: DestinationsNavigator,
    loginViewModel: LoginViewModel = hiltViewModel()
) {
    val delegatedUiState by loginViewModel.uiState.collectAsState()
    val delegatedQuickConnectUiState by loginViewModel.quickConnectUiState.collectAsState(
        initial = LoginViewModel.QuickConnectUiState.Disabled
    )

    val navigateToHome by loginViewModel.navigateToMain.collectAsState(initial = false)
    if (navigateToHome) {
        navigator.navigate(HomeScreenDestination)
    }

    var username by rememberSaveable {
        mutableStateOf("")
    }
    var password by rememberSaveable {
        mutableStateOf("")
    }

    var quickConnectValue = stringResource(id = CoreR.string.quick_connect)

    when (val quickConnectUiState = delegatedQuickConnectUiState) {
        is LoginViewModel.QuickConnectUiState.Waiting -> {
            quickConnectValue = quickConnectUiState.code
        }
        else -> Unit
    }

    val isError = delegatedUiState is LoginViewModel.UiState.Error
    val isLoading = delegatedUiState is LoginViewModel.UiState.Loading

    val quickConnectEnabled = delegatedQuickConnectUiState !is LoginViewModel.QuickConnectUiState.Disabled
    val isWaiting = delegatedQuickConnectUiState is LoginViewModel.QuickConnectUiState.Waiting

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(Color.Black, Color(0xFF001721))))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
        ) {
            Text(
                text = stringResource(id = CoreR.string.login),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(32.dp))
            OutlinedTextField(
                value = username,
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = CoreR.drawable.ic_user),
                        contentDescription = null
                    )
                },
                onValueChange = { username = it },
                label = { Text(text = stringResource(id = CoreR.string.edit_text_username_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    autoCorrect = false,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                isError = isError,
                enabled = !isLoading,
                modifier = Modifier
                    .width(360.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = password,
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = CoreR.drawable.ic_lock),
                        contentDescription = null
                    )
                },
                onValueChange = { password = it },
                label = { Text(text = stringResource(id = CoreR.string.edit_text_password_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    autoCorrect = false,
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Go
                ),
                visualTransformation = PasswordVisualTransformation(),
                isError = isError,
                enabled = !isLoading,
                supportingText = {
                    if (isError) {
                        // TODO fix `asString()` composable not working
                        Text(
                            text = (delegatedUiState as LoginViewModel.UiState.Error).message.asString(
                                LocalContext.current.resources
                            ),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                modifier = Modifier
                    .width(360.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Box {
                Button(
                    onClick = {
                        loginViewModel.login(username, password)
                    },
                    enabled = !isLoading,
                    modifier = Modifier.width(360.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = LocalContentColor.current,
                                modifier = Modifier
                                    .size(24.dp)
                            )
                        }
                        Text(
                            text = stringResource(id = CoreR.string.button_login),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
            if (quickConnectEnabled) {
                Spacer(modifier = Modifier.height(16.dp))
                Box {
                    OutlinedButton(
                        onClick = {
                            loginViewModel.useQuickConnect()
                        },
                        modifier = Modifier.width(360.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isWaiting) {
                                CircularProgressIndicator(
                                    color = LocalContentColor.current,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .align(Alignment.CenterStart)
                                )
                            }
                            Text(
                                text = quickConnectValue,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }
                }
            }
        }
    }
}
