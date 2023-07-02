package dev.jdtech.jellyfin.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.models.Server
import dev.jdtech.jellyfin.models.User
import dev.jdtech.jellyfin.ui.destinations.HomeScreenDestination
import dev.jdtech.jellyfin.ui.destinations.LoginScreenDestination
import dev.jdtech.jellyfin.viewmodels.UserSelectViewModel
import org.jellyfin.sdk.model.api.ImageType
import java.util.UUID

@OptIn(ExperimentalTvMaterial3Api::class)
@Destination
@Composable
fun UserSelectScreen(
    serverId: String,
    navigator: DestinationsNavigator,
    userSelectViewModel: UserSelectViewModel = hiltViewModel()
) {
    val delegatedUiState by userSelectViewModel.uiState.collectAsState()
    val navigateToHome by userSelectViewModel.navigateToMain.collectAsState(initial = false)
    if (navigateToHome) {
        navigator.navigate(HomeScreenDestination)
    }

    var server: Server? = null
    var users: List<User> = emptyList()

    when (val uiState = delegatedUiState) {
        is UserSelectViewModel.UiState.Normal -> {
            server = uiState.server
            users = uiState.users
        }

        else -> Unit
    }

    LaunchedEffect(key1 = true) {
        userSelectViewModel.loadUsers(serverId)
    }

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
                text = stringResource(id = CoreR.string.select_user),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold
            )
            server?.let {
                Text(
                    text = "Server: ${it.name}",
                    color = Color(0xFFBDBDBD)
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            if (users.isEmpty()) {
                Text(
                    text = stringResource(id = CoreR.string.no_users_found),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium
                )
            } else {
                TvLazyRow(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    contentPadding = PaddingValues(24.dp)
                ) {
                    items(users) {
                        UserComponent(it) { user ->
                            userSelectViewModel.loginAsUser(user)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            OutlinedButton(
                onClick = {
                    navigator.navigate(LoginScreenDestination)
                }
            ) {
                Text(text = stringResource(id = CoreR.string.add_user))
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun UserComponent(
    user: User,
    onClick: (User) -> Unit = {}
) {
    val context = LocalContext.current
    val api = JellyfinApi.getInstance(context)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(120.dp)
    ) {
        Surface(
            onClick = {
                onClick(user)
            },
            colors = ClickableSurfaceDefaults.colors(
                containerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent
            ),
            border = ClickableSurfaceDefaults.border(
                border = Border(BorderStroke(1.dp, Color.White), shape = CircleShape),
                focusedBorder = Border(BorderStroke(4.dp, Color.White), shape = CircleShape)
            ),
            shape = ClickableSurfaceDefaults.shape(
                shape = CircleShape,
                focusedShape = CircleShape
            ),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        ) {
            Icon(
                painter = painterResource(id = CoreR.drawable.ic_user),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .width(48.dp)
                    .height(48.dp)
                    .align(Alignment.Center)
            )
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data("${api.api.baseUrl}/users/${user.id}/Images/${ImageType.PRIMARY}")
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = user.name,
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Preview
@Composable
private fun UserPreview() {
    UserComponent(user = User(id = UUID.randomUUID(), name = "Bob", serverId = ""))
}
