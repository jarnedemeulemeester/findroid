package dev.jdtech.jellyfin

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.Navigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import dev.jdtech.jellyfin.presentation.setup.addserver.AddServerScreen
import dev.jdtech.jellyfin.presentation.setup.login.LoginScreen
import dev.jdtech.jellyfin.presentation.setup.servers.ServersScreen
import dev.jdtech.jellyfin.presentation.setup.users.UsersScreen
import dev.jdtech.jellyfin.presentation.setup.welcome.WelcomeScreen
import kotlinx.serialization.Serializable

@Serializable
data object WelcomeRoute

@Serializable
data object ServersRoute

@Serializable
data object AddServerRoute

@Serializable
data object UsersRoute

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
        hasServers && hasCurrentServer && hasCurrentUser -> UsersRoute // TODO: change to MainRoute
        hasServers && hasCurrentServer -> UsersRoute
        hasServers -> ServersRoute
        else -> WelcomeRoute
    }
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            fadeIn(tween(300))
        },
        exitTransition = {
            fadeOut(tween(300))
        },
    ) {
        composable<WelcomeRoute> {
            WelcomeScreen(
                onContinueClick = {
                    navController.safeNavigate(ServersRoute)
                },
            )
        }
        composable<ServersRoute> { backStackEntry ->
            ServersScreen(
                navigateToLogin = {
                    navController.safeNavigate(LoginRoute)
                },
                navigateToUsers = {
                    navController.safeNavigate(UsersRoute)
                },
                onAddClick = {
                    navController.safeNavigate(AddServerRoute)
                },
                onBackClick = {
                    navController.safePopBackStack()
                },
                showBack = navController.previousBackStackEntry != null && navController.previousBackStackEntry != backStackEntry,
            )
        }
        composable<AddServerRoute> {
            AddServerScreen(
                onSuccess = {
                    navController.safeNavigate(LoginRoute)
                },
                onBackClick = {
                    navController.safePopBackStack()
                },
            )
        }
        composable<UsersRoute> { backStackEntry ->
            UsersScreen(
                navigateToHome = {},
                onChangeServerClick = {
                    navController.safeNavigate(ServersRoute) {
                        popUpTo(ServersRoute) {
                            inclusive = false
                        }
                        launchSingleTop = true
                    }
                },
                onAddClick = {
                    navController.safeNavigate(LoginRoute)
                },
                onBackClick = {
                    navController.safePopBackStack()
                },
                showBack = navController.previousBackStackEntry != null && navController.previousBackStackEntry != backStackEntry,
            )
        }
        composable<LoginRoute> {
            LoginScreen(
                onSuccess = {},
                onChangeServerClick = {
                    navController.safeNavigate(ServersRoute) {
                        popUpTo(ServersRoute) {
                            inclusive = false
                        }
                        launchSingleTop = true
                    }
                },
                onBackClick = {
                    navController.safePopBackStack()
                },
            )
        }
    }
}

private fun <T : Any> NavHostController.safeNavigate(route: T, navOptions: NavOptions? = null, navigatorExtras: Navigator.Extras? = null) {
    if (this.currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
        this.navigate(route, navOptions, navigatorExtras)
    }
}

private fun <T : Any> NavHostController.safeNavigate(route: T, builder: NavOptionsBuilder.() -> Unit) {
    if (this.currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
        this.navigate(route, builder)
    }
}

private fun NavHostController.safePopBackStack(): Boolean {
    return if (this.currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
        this.popBackStack()
    } else {
        false
    }
}
