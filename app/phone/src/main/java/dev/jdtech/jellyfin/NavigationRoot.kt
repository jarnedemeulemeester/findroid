package dev.jdtech.jellyfin

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import dev.jdtech.jellyfin.presentation.setup.addserver.AddServerScreen
import dev.jdtech.jellyfin.presentation.setup.login.LoginScreen
import dev.jdtech.jellyfin.presentation.setup.servers.ServersScreen
import dev.jdtech.jellyfin.presentation.setup.users.UsersScreen
import dev.jdtech.jellyfin.presentation.setup.welcome.WelcomeScreen
import kotlinx.serialization.Serializable

@Serializable
data object WelcomeRoute

@Serializable
data class ServersRoute(
    val showBack: Boolean = true,
)

@Serializable
data object AddServerRoute

@Serializable
data class UsersRoute(
    val showBack: Boolean = true,
)

@Serializable
data object LoginRoute

@Composable
fun NavigationRoot(
    navController: NavHostController,
    hasServers: Boolean,
    hasCurrentServer: Boolean,
    hasCurrentUser: Boolean,
) {
    val startDestination = when {
        hasServers && hasCurrentServer && hasCurrentUser -> UsersRoute(showBack = false) // TODO: change to MainRoute
        hasServers && hasCurrentServer -> UsersRoute(showBack = false)
        hasServers -> ServersRoute(showBack = false)
        else -> WelcomeRoute
    }
    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable<WelcomeRoute> {
            WelcomeScreen(
                onContinueClick = {
                    navController.navigate(ServersRoute())
                },
            )
        }
        composable<ServersRoute> { backStackEntry ->
            val route: ServersRoute = backStackEntry.toRoute()
            ServersScreen(
                navigateToLogin = {
                    navController.navigate(LoginRoute)
                },
                navigateToUsers = {
                    navController.navigate(UsersRoute())
                },
                onAddClick = {
                    navController.navigate(AddServerRoute)
                },
                onBackClick = {
                    navController.popBackStack()
                },
                showBack = route.showBack,
            )
        }
        composable<AddServerRoute> {
            AddServerScreen(
                onSuccess = {
                    navController.navigate(LoginRoute)
                },
                onBackClick = {
                    navController.popBackStack()
                },
            )
        }
        composable<UsersRoute> { backStackEntry ->
            val route: UsersRoute = backStackEntry.toRoute()
            UsersScreen(
                navigateToHome = {},
                onChangeServerClick = {
                    navController.navigate(ServersRoute()) {
                        popUpTo(ServersRoute()) {
                            inclusive = false
                        }
                        launchSingleTop = true
                    }
                },
                onAddClick = {
                    navController.navigate(LoginRoute)
                },
                onBackClick = {
                    navController.popBackStack()
                },
                showBack = route.showBack,
            )
        }
        composable<LoginRoute> {
            LoginScreen(
                onSuccess = {},
                onChangeServerClick = {
                    navController.navigate(ServersRoute()) {
                        popUpTo(ServersRoute()) {
                            inclusive = false
                        }
                        launchSingleTop = true
                    }
                },
                onBackClick = {
                    navController.popBackStack()
                },
            )
        }
    }
}
