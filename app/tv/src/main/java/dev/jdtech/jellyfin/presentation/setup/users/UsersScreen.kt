package dev.jdtech.jellyfin.presentation.setup.users

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.NavGraphs
import com.ramcosta.composedestinations.generated.destinations.LoginScreenDestination
import com.ramcosta.composedestinations.generated.destinations.MainScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.presentation.setup.components.UserItem
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.setup.presentation.users.UsersAction
import dev.jdtech.jellyfin.setup.presentation.users.UsersEvent
import dev.jdtech.jellyfin.setup.presentation.users.UsersState
import dev.jdtech.jellyfin.setup.presentation.users.UsersViewModel
import dev.jdtech.jellyfin.ui.dummy.dummyUsers
import dev.jdtech.jellyfin.utils.ObserveAsEvents
import dev.jdtech.jellyfin.setup.R as SetupR

@Destination<RootGraph>
@Composable
fun UserSelectScreen(
    navigator: DestinationsNavigator,
    viewModel: UsersViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val api = JellyfinApi.getInstance(context)
    val state by viewModel.state.collectAsState()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is UsersEvent.NavigateToHome -> {
                navigator.navigate(MainScreenDestination) {
                    popUpTo(NavGraphs.root) {
                        inclusive = true
                    }
                }
            }
            else -> Unit
        }
    }

    LaunchedEffect(true) {
        viewModel.loadUsers()
    }

    UserSelectScreenLayout(
        state = state,
        onAction = { action ->
            when (action) {
                is UsersAction.OnChangeServerClick -> Unit
                is UsersAction.OnAddClick -> {
                    navigator.navigate(LoginScreenDestination)
                }
                else -> Unit
            }
            viewModel.onAction(action)
        },
        api.api.baseUrl ?: "",
    )
}

@Composable
private fun UserSelectScreenLayout(
    state: UsersState,
    onAction: (UsersAction) -> Unit,
    baseUrl: String,
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
                text = stringResource(id = SetupR.string.users),
                style = MaterialTheme.typography.displayMedium,
            )
            Text(
                text = stringResource(SetupR.string.server_subtitle, state.serverName ?: ""),
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFBDBDBD),
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacings.large))
            if (state.users.isEmpty()) {
                Text(
                    text = stringResource(id = SetupR.string.users_no_users),
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.default),
                    contentPadding = PaddingValues(MaterialTheme.spacings.default),
                    modifier = Modifier.focusRequester(focusRequester),
                ) {
                    items(state.users) {
                        UserItem(
                            user = it,
                            onClick = { user ->
                                onAction(UsersAction.OnUserClick(user.id))
                            },
                            baseUrl = baseUrl,
                        )
                    }
                }
                LaunchedEffect(true) {
                    focusRequester.requestFocus()
                }
            }
            Spacer(modifier = Modifier.height(MaterialTheme.spacings.large))
            OutlinedButton(
                onClick = {
                    onAction(UsersAction.OnAddClick)
                },
            ) {
                Text(text = stringResource(id = SetupR.string.users_btn_add_user))
            }
        }
    }
}

@Preview(device = "id:tv_1080p")
@Composable
private fun UserSelectScreenLayoutPreview() {
    FindroidTheme {
        UserSelectScreenLayout(
            state = UsersState(users = dummyUsers, serverName = "Demo"),
            onAction = {},
            baseUrl = "https://demo.jellyfin.org/stable",
        )
    }
}

@Preview(device = "id:tv_1080p")
@Composable
private fun UserSelectScreenLayoutPreviewNoUsers() {
    FindroidTheme {
        UserSelectScreenLayout(
            state = UsersState(serverName = "Demo"),
            onAction = {},
            baseUrl = "https://demo.jellyfin.org/stable",
        )
    }
}
