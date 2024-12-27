package dev.jdtech.jellyfin

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.Navigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navigation
import dev.jdtech.jellyfin.presentation.film.HomeScreen
import dev.jdtech.jellyfin.presentation.film.MediaScreen
import dev.jdtech.jellyfin.presentation.setup.addserver.AddServerScreen
import dev.jdtech.jellyfin.presentation.setup.login.LoginScreen
import dev.jdtech.jellyfin.presentation.setup.servers.ServersScreen
import dev.jdtech.jellyfin.presentation.setup.users.UsersScreen
import dev.jdtech.jellyfin.presentation.setup.welcome.WelcomeScreen
import kotlinx.serialization.Serializable
import dev.jdtech.jellyfin.core.R as CoreR

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

@Serializable
data object FilmGraphRoute

@Serializable
data object HomeRoute

@Serializable
data object MediaRoute

data class TabBarItem(
    val title: String,
    @DrawableRes val icon: Int,
    val route: Any,
)

val homeTab = TabBarItem(title = "Home", icon = CoreR.drawable.ic_home, route = HomeRoute)
val mediaTab = TabBarItem(title = "Media", icon = CoreR.drawable.ic_library, route = MediaRoute)
// val downloadsTab = TabBarItem(title = "Downloads", icon = CoreR.drawable.ic_download, route = Unit)

val tabBarItems = listOf(homeTab, mediaTab)

@Composable
fun NavigationRoot(
    navController: NavHostController,
    hasServers: Boolean,
    hasCurrentServer: Boolean,
    hasCurrentUser: Boolean,
) {
    val startDestination = when {
        hasServers && hasCurrentServer && hasCurrentUser -> FilmGraphRoute
        hasServers && hasCurrentServer -> UsersRoute
        hasServers -> ServersRoute
        else -> WelcomeRoute
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in tabBarItems.map { it.route::class.qualifiedName }

    val adaptiveInfo = currentWindowAdaptiveInfo()
    val customNavSuiteType = with(adaptiveInfo) {
        if (!showBottomBar) {
            NavigationSuiteType.None
        } else {
            NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(adaptiveInfo)
        }
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            tabBarItems.forEachIndexed { index, item ->
                item(
                    selected = currentRoute == item.route::class.qualifiedName,
                    onClick = {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = {
                        Icon(
                            painter = painterResource(item.icon),
                            contentDescription = item.title,
                        )
                    },
                    label = {
                        Text(text = item.title)
                    },
                    alwaysShowLabel = false,
                )
            }
        },
        layoutType = customNavSuiteType,
    ) {
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
                    showBack = navController.previousBackStackEntry != null,
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
                    showBack = navController.previousBackStackEntry != null,
                )
            }
            composable<LoginRoute> {
                LoginScreen(
                    onSuccess = {
                        navController.safeNavigate(FilmGraphRoute) {
                            popUpTo(0)
                            launchSingleTop = true
                        }
                    },
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
            navigation<FilmGraphRoute>(
                startDestination = HomeRoute,
            ) {
                composable<HomeRoute> {
                    HomeScreen()
                }
                composable<MediaRoute> {
                    MediaScreen()
                }
            }
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
