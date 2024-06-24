package dev.jdtech.jellyfin.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.navigation.popUpTo
import dev.jdtech.jellyfin.NavGraphs
import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.destinations.LoginScreenDestination
import dev.jdtech.jellyfin.destinations.MainScreenDestination
import dev.jdtech.jellyfin.models.Server
import dev.jdtech.jellyfin.models.User
import dev.jdtech.jellyfin.ui.dummy.dummyServer
import dev.jdtech.jellyfin.ui.dummy.dummyUser
import dev.jdtech.jellyfin.ui.dummy.dummyUsers
import dev.jdtech.jellyfin.ui.theme.FindroidTheme
import dev.jdtech.jellyfin.ui.theme.spacings
import dev.jdtech.jellyfin.utils.ObserveAsEvents
import dev.jdtech.jellyfin.viewmodels.UserSelectEvent
import dev.jdtech.jellyfin.viewmodels.UserSelectViewModel
import org.jellyfin.sdk.model.api.ImageType
import dev.jdtech.jellyfin.core.R as CoreR

@Destination
@Composable
fun UserSelectScreen(
    navigator: DestinationsNavigator,
    userSelectViewModel: UserSelectViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val api = JellyfinApi.getInstance(context)
    val delegatedUiState by userSelectViewModel.uiState.collectAsState()

    ObserveAsEvents(userSelectViewModel.eventsChannelFlow) { event ->
        when (event) {
            is UserSelectEvent.NavigateToMain -> {
                navigator.navigate(MainScreenDestination) {
                    popUpTo(NavGraphs.root) {
                        inclusive = true
                    }
                }
            }
        }
    }

    LaunchedEffect(key1 = true) {
        userSelectViewModel.loadUsers()
    }

    UserSelectScreenLayout(
        uiState = delegatedUiState,
        baseUrl = api.api.baseUrl ?: "",
        onUserClick = { user ->
            userSelectViewModel.loginAsUser(user)
        },
        onAddUserClick = {
            navigator.navigate(LoginScreenDestination)
        },
    )
}

@Composable
private fun UserSelectScreenLayout(
    uiState: UserSelectViewModel.UiState,
    baseUrl: String,
    onUserClick: (User) -> Unit,
    onAddUserClick: () -> Unit,
) {
    var server: Server? = null
    var users: List<User> = emptyList()

    when (uiState) {
        is UserSelectViewModel.UiState.Normal -> {
            server = uiState.server
            users = uiState.users
        }
        else -> Unit
    }

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
                text = stringResource(id = CoreR.string.select_user),
                style = MaterialTheme.typography.displayMedium,
            )
            server?.let {
                Text(
                    text = "Server: ${it.name}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFBDBDBD),
                )
            }
            Spacer(modifier = Modifier.height(MaterialTheme.spacings.large))
            if (users.isEmpty()) {
                Text(
                    text = stringResource(id = CoreR.string.no_users_found),
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                TvLazyRow(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.default),
                    contentPadding = PaddingValues(MaterialTheme.spacings.default),
                    modifier = Modifier.focusRequester(focusRequester),
                ) {
                    items(users) {
                        UserComponent(
                            user = it,
                            baseUrl = baseUrl,
                        ) { user ->
                            onUserClick(user)
                        }
                    }
                }
                LaunchedEffect(true) {
                    focusRequester.requestFocus()
                }
            }
            Spacer(modifier = Modifier.height(MaterialTheme.spacings.large))
            OutlinedButton(
                onClick = {
                    onAddUserClick()
                },
            ) {
                Text(text = stringResource(id = CoreR.string.add_user))
            }
        }
    }
}

@Preview(device = "id:tv_1080p")
@Composable
private fun UserSelectScreenLayoutPreview() {
    FindroidTheme {
        UserSelectScreenLayout(
            uiState = UserSelectViewModel.UiState.Normal(dummyServer, dummyUsers),
            baseUrl = "https://demo.jellyfin.org/stable",
            onUserClick = {},
            onAddUserClick = {},
        )
    }
}

@Preview(device = "id:tv_1080p")
@Composable
private fun UserSelectScreenLayoutPreviewNoUsers() {
    FindroidTheme {
        UserSelectScreenLayout(
            uiState = UserSelectViewModel.UiState.Normal(dummyServer, emptyList()),
            baseUrl = "https://demo.jellyfin.org/stable",
            onUserClick = {},
            onAddUserClick = {},
        )
    }
}

@Composable
private fun UserComponent(
    user: User,
    baseUrl: String,
    onClick: (User) -> Unit = {},
) {
    val context = LocalContext.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(120.dp),
    ) {
        Surface(
            onClick = {
                onClick(user)
            },
            colors = ClickableSurfaceDefaults.colors(
                containerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
            ),
            border = ClickableSurfaceDefaults.border(
                border = Border(BorderStroke(1.dp, Color.White), shape = CircleShape),
                focusedBorder = Border(BorderStroke(4.dp, Color.White), shape = CircleShape),
            ),
            shape = ClickableSurfaceDefaults.shape(
                shape = CircleShape,
                focusedShape = CircleShape,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
        ) {
            Icon(
                painter = painterResource(id = CoreR.drawable.ic_user),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .width(48.dp)
                    .height(48.dp)
                    .align(Alignment.Center),
            )
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data("$baseUrl/users/${user.id}/Images/${ImageType.PRIMARY}")
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Spacer(modifier = Modifier.height(MaterialTheme.spacings.medium))
        Text(
            text = user.name,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Preview
@Composable
private fun UserComponentPreview() {
    FindroidTheme {
        UserComponent(
            user = dummyUser,
            baseUrl = "https://demo.jellyfin.org/stable",
        )
    }
}
