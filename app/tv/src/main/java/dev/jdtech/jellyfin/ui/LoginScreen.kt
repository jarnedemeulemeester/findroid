package dev.jdtech.jellyfin.ui

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.models.User
import dev.jdtech.jellyfin.ui.components.Banner
import dev.jdtech.jellyfin.ui.theme.Typography
import dev.jdtech.jellyfin.viewmodels.LoginViewModel

@Destination
@Composable
fun LoginScreen(
    navigator: DestinationsNavigator,
    loginViewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by loginViewModel.uiState.collectAsState()
    val usersState by loginViewModel.usersState.collectAsState()
    val quickConnectUiState by loginViewModel.quickConnectUiState.collectAsState(initial = LoginViewModel.QuickConnectUiState.Disabled)

    val navigateToHome by loginViewModel.navigateToMain.collectAsState(initial = false)
    if (navigateToHome) {
        // TODO navigate to home
    }
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxSize()
    ) {
        Banner()
        LoginForm(
            uiState = uiState,
            usersState = usersState,
            quickConnectUiState = quickConnectUiState,
            onSubmit = { username, password ->
                loginViewModel.login(username, password)
            },
            onQuickConnect = {
                loginViewModel.useQuickConnect()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginForm(
    uiState: LoginViewModel.UiState,
    usersState: LoginViewModel.UsersState,
    quickConnectUiState: LoginViewModel.QuickConnectUiState,
    onSubmit: (String, String) -> Unit,
    onQuickConnect: () -> Unit
) {
    var username by rememberSaveable {
        mutableStateOf("")
    }
    var password by rememberSaveable {
        mutableStateOf("")
    }
    val users =
        if (usersState is LoginViewModel.UsersState.Users) {
            usersState.users
        } else emptyList()
    val quickConnectValue = if (quickConnectUiState is LoginViewModel.QuickConnectUiState.Waiting) {
        quickConnectUiState.code
    } else stringResource(id = R.string.quick_connect)

    val isError = uiState is LoginViewModel.UiState.Error
    val isLoading = uiState is LoginViewModel.UiState.Loading

    val isWaiting = quickConnectUiState is LoginViewModel.QuickConnectUiState.Waiting

    val context = LocalContext.current

    val requester = FocusRequester()

    Column(Modifier.width(320.dp)) {
        Text(text = stringResource(id = R.string.login), style = Typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))
        AnimatedVisibility(visible = users.isNotEmpty()) {
            Column {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(users) { _, user ->
                        PublicUserComponent(user = user) {
                            username = it.name
                            requester.requestFocus()
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
        OutlinedTextField(
            value = username,
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_user),
                    contentDescription = null
                )
            },
            onValueChange = { username = it },
            label = { Text(text = stringResource(id = R.string.edit_text_username_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                autoCorrect = false,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            isError = isError,
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
        )
        OutlinedTextField(
            value = password,
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_lock),
                    contentDescription = null
                )
            },
            onValueChange = { password = it },
            label = { Text(text = stringResource(id = R.string.edit_text_password_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                autoCorrect = false,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Go
            ),
            visualTransformation = PasswordVisualTransformation(),
            isError = isError,
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(requester)
        )
        Text(
            text = if (isError) (uiState as LoginViewModel.UiState.Error).message.asString(context.resources) else "",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box {
            Button(
                onClick = {
                    onSubmit(username, password)
                },
                enabled = !isLoading,
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
        AnimatedVisibility(visible = quickConnectUiState !is LoginViewModel.QuickConnectUiState.Disabled) {
            Box {
                OutlinedButton(
                    onClick = {
                        onQuickConnect()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = quickConnectValue)
                }
                if (isWaiting) {
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
}

@Composable
fun PublicUserComponent(
    user: User,
    onClick: (User) -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(64.dp)
            .clip(MaterialTheme.shapes.small)
            .clickable { onClick(user) }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.primary)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_user),
                contentDescription = null,
                modifier = Modifier.align(Alignment.Center),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = user.name,
            style = Typography.bodyMedium,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1
        )
    }
}
