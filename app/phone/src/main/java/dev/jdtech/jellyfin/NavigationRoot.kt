package dev.jdtech.jellyfin

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import dev.jdtech.jellyfin.presentation.setup.addserver.AddServerScreen
import dev.jdtech.jellyfin.presentation.setup.discoverserver.DiscoverServerScreen
import dev.jdtech.jellyfin.presentation.setup.login.LoginScreen
import dev.jdtech.jellyfin.presentation.setup.welcome.WelcomeScreen
import kotlinx.serialization.Serializable

@Serializable
data object WelcomeRoute

@Serializable
data object DiscoverServerRoute

@Serializable
data object AddServerRoute

@Serializable
data object LoginRoute

@Composable
fun NavigationRoot(
    navController: NavHostController,
    hasServers: Boolean,
    isLoggedIn: Boolean,
) {
    NavHost(
        navController = navController,
        startDestination = when {
            hasServers && !isLoggedIn -> LoginRoute
            else -> WelcomeRoute
        },
    ) {
        composable<WelcomeRoute> {
            WelcomeScreen(onContinueClick = {
                navController.navigate(DiscoverServerRoute)
            })
        }
        composable<DiscoverServerRoute> {
            DiscoverServerScreen(
                onSuccess = {
                    navController.navigate(LoginRoute)
                },
                onManualClick = {
                    navController.navigate(AddServerRoute)
                },
                onBackClick = {
                    navController.popBackStack()
                },
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
        composable<LoginRoute> {
            LoginScreen(
                onSuccess = {},
                onBackClick = {
                    navController.popBackStack()
                },
            )
        }
    }
}
