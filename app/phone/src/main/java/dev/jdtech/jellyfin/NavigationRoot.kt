package dev.jdtech.jellyfin

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.adaptive.navigationsuite.rememberNavigationSuiteScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.Navigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import androidx.window.core.layout.WindowSizeClass
import dev.jdtech.jellyfin.models.CollectionType
import dev.jdtech.jellyfin.models.FindroidBoxSet
import dev.jdtech.jellyfin.models.FindroidCollection
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.FindroidMovie
import dev.jdtech.jellyfin.models.FindroidSeason
import dev.jdtech.jellyfin.models.FindroidShow
import dev.jdtech.jellyfin.presentation.film.CollectionScreen
import dev.jdtech.jellyfin.presentation.film.EpisodeScreen
import dev.jdtech.jellyfin.presentation.film.FavoritesScreen
import dev.jdtech.jellyfin.presentation.film.HomeScreen
import dev.jdtech.jellyfin.presentation.film.LibraryScreen
import dev.jdtech.jellyfin.presentation.film.MediaScreen
import dev.jdtech.jellyfin.presentation.film.MovieScreen
import dev.jdtech.jellyfin.presentation.film.PersonScreen
import dev.jdtech.jellyfin.presentation.film.SeasonScreen
import dev.jdtech.jellyfin.presentation.film.ShowScreen
import dev.jdtech.jellyfin.presentation.settings.AboutScreen
import dev.jdtech.jellyfin.presentation.settings.SettingsScreen
import dev.jdtech.jellyfin.presentation.setup.addresses.ServerAddressesScreen
import dev.jdtech.jellyfin.presentation.setup.addserver.AddServerScreen
import dev.jdtech.jellyfin.presentation.setup.login.LoginScreen
import dev.jdtech.jellyfin.presentation.setup.servers.ServersScreen
import dev.jdtech.jellyfin.presentation.setup.users.UsersScreen
import dev.jdtech.jellyfin.presentation.setup.welcome.WelcomeScreen
import kotlinx.serialization.Serializable
import java.util.UUID
import timber.log.Timber
import dev.jdtech.jellyfin.core.R as CoreR

@Serializable
data object WelcomeRoute

@Serializable
data object ServersRoute

@Serializable
data object AddServerRoute

@Serializable
data class ServerAddressesRoute(
    val serverId: String,
)

@Serializable
data object UsersRoute

@Serializable
data class LoginRoute(
    val username: String? = null,
)

@Serializable
data object HomeRoute

@Serializable
data object MediaRoute

@Serializable
data object DownloadsRoute

@Serializable
data class LibraryRoute(
    val libraryId: String,
    val libraryName: String,
    val libraryType: CollectionType,
)

@Serializable
data class CollectionRoute(
    val collectionId: String,
    val collectionName: String,
    val onePerGenre: Boolean = false,
)

@Serializable
data object FavoritesRoute

@Serializable
data class MovieRoute(
    val movieId: String,
)

@Serializable
data class ShowRoute(
    val showId: String,
)

@Serializable
data class EpisodeRoute(
    val episodeId: String,
)

@Serializable
data class SeasonRoute(
    val seasonId: String,
)

@Serializable
data class PersonRoute(
    val personId: String,
)

@Serializable
data class SettingsRoute(
    val indexes: IntArray,
)

@Serializable
data object AboutRoute

data class TabBarItem(
    @param:StringRes val title: Int,
    @param:DrawableRes val icon: Int,
    val route: Any,
    val enabled: Boolean = true,
)

val homeTab = TabBarItem(title = CoreR.string.title_home, icon = CoreR.drawable.ic_home, route = HomeRoute)
val mediaTab = TabBarItem(title = CoreR.string.title_media, icon = CoreR.drawable.ic_library, route = MediaRoute)
val downloadsTab = TabBarItem(title = CoreR.string.title_download, icon = CoreR.drawable.ic_download, route = DownloadsRoute, enabled = true)

val navigationItems = listOf(homeTab, mediaTab, downloadsTab)
val navigationItemClassNames = navigationItems.map { it.route::class.qualifiedName }

@Composable
fun NavigationRoot(
    navController: NavHostController,
    hasServers: Boolean,
    hasCurrentServer: Boolean,
    hasCurrentUser: Boolean,
) {
    val startDestination = when {
        hasServers && hasCurrentServer && hasCurrentUser -> HomeRoute
        hasServers && hasCurrentServer -> UsersRoute
        hasServers -> ServersRoute
        else -> WelcomeRoute
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()

    var searchExpanded by remember { mutableStateOf(false) }

    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in navigationItemClassNames && !searchExpanded

    val navigationSuiteScaffoldState = rememberNavigationSuiteScaffoldState()

    // Log current page whenever the route changes
    LaunchedEffect(currentRoute) {
        val pageName = when (currentRoute) {
            WelcomeRoute::class.qualifiedName -> "Welcome"
            ServersRoute::class.qualifiedName -> "Servers"
            AddServerRoute::class.qualifiedName -> "AddServer"
            UsersRoute::class.qualifiedName -> "Users"
            LoginRoute::class.qualifiedName -> "Login"
            HomeRoute::class.qualifiedName -> "Home"
            MediaRoute::class.qualifiedName -> "Media"
            DownloadsRoute::class.qualifiedName -> "Downloads"
            LibraryRoute::class.qualifiedName -> "Library"
            CollectionRoute::class.qualifiedName -> "Collection"
            FavoritesRoute::class.qualifiedName -> "Favorites"
            MovieRoute::class.qualifiedName -> "Movie"
            ShowRoute::class.qualifiedName -> "Show"
            EpisodeRoute::class.qualifiedName -> "Episode"
            SeasonRoute::class.qualifiedName -> "Season"
            PersonRoute::class.qualifiedName -> "Person"
            SettingsRoute::class.qualifiedName -> "Settings"
            AboutRoute::class.qualifiedName -> "About"
            else -> currentRoute ?: "Unknown"
        }
        Timber.tag("Nav").d("Current page: %s", pageName)
    }

    LaunchedEffect(showBottomBar) {
        if (showBottomBar) {
            navigationSuiteScaffoldState.show()
        } else {
            navigationSuiteScaffoldState.hide()
        }
    }

    val windowAdaptiveInfo = currentWindowAdaptiveInfo()
    val customNavSuiteType = with(windowAdaptiveInfo) {
        if (windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)) {
            NavigationSuiteType.NavigationRail
        } else {
            NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(this)
        }
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            navigationItems.forEach { item ->
                item(
                    selected = currentRoute == item.route::class.qualifiedName,
                    onClick = {
                        if (item.route is MediaRoute && currentRoute == MediaRoute::class.qualifiedName) {
                            searchExpanded = true
                        }

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
                            contentDescription = stringResource(item.title),
                        )
                    },
                    enabled = item.enabled,
                    label = {
                        Text(text = stringResource(item.title))
                    },
                )
            }
        },
        layoutType = customNavSuiteType,
        state = navigationSuiteScaffoldState,
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
            composable<DownloadsRoute> { backStackEntry ->
                // Force recomposition when navigating back by using a key
                androidx.compose.runtime.key(backStackEntry.id) {
                    // Host the existing Fragment-based Downloads screen inside Compose
                    dev.jdtech.jellyfin.presentation.downloads.DownloadsScreenHost(
                        modifier = androidx.compose.ui.Modifier.fillMaxSize()
                    )
                }
            }
            composable<ServersRoute> { backStackEntry ->
                ServersScreen(
                    navigateToUsers = {
                        navController.safeNavigate(UsersRoute)
                    },
                    navigateToAddresses = { serverId ->
                        navController.safeNavigate(ServerAddressesRoute(serverId))
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
                        navController.safeNavigate(UsersRoute)
                    },
                    onBackClick = {
                        navController.safePopBackStack()
                    },
                )
            }
            composable<ServerAddressesRoute> { backStackEntry ->
                val route: ServerAddressesRoute = backStackEntry.toRoute()
                ServerAddressesScreen(
                    serverId = route.serverId,
                    navigateBack = {
                        navController.safePopBackStack()
                    },
                )
            }
            composable<UsersRoute> { backStackEntry ->
                UsersScreen(
                    navigateToHome = {
                        navController.safeNavigate(HomeRoute) {
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
                    onAddClick = {
                        navController.safeNavigate(LoginRoute())
                    },
                    onBackClick = {
                        navController.safePopBackStack()
                    },
                    onPublicUserClick = { username ->
                        navController.safeNavigate(LoginRoute(username = username))
                    },
                    showBack = navController.previousBackStackEntry != null,
                )
            }
            composable<LoginRoute> { backStackEntry ->
                val route: LoginRoute = backStackEntry.toRoute()
                LoginScreen(
                    onSuccess = {
                        navController.safeNavigate(HomeRoute) {
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
                    prefilledUsername = route.username,
                )
            }
            composable<HomeRoute> {
                HomeScreen(
                    onLibraryClick = {
                        navController.safeNavigate(LibraryRoute(libraryId = it.id.toString(), libraryName = it.name, libraryType = it.type))
                    },
                    onSettingsClick = {
                        navController.safeNavigate(SettingsRoute(indexes = intArrayOf(CoreR.string.title_settings)))
                    },
                    onManageServers = {
                        navController.safeNavigate(ServersRoute)
                    },
                    onItemClick = { item ->
                        navigateToItem(navController = navController, item = item)
                    },
                )
            }
            composable<MediaRoute> {
                MediaScreen(
                    onItemClick = { item ->
                        navigateToItem(navController = navController, item = item)
                    },
                    onFavoritesClick = {
                        navController.safeNavigate(FavoritesRoute)
                    },
                    searchExpanded = searchExpanded,
                    onSearchExpand = { searchExpanded = it },
                )
            }
            composable<LibraryRoute> { backStackEntry ->
                val route: LibraryRoute = backStackEntry.toRoute()
                LibraryScreen(
                    libraryId = UUID.fromString(route.libraryId),
                    libraryName = route.libraryName,
                    libraryType = route.libraryType,
                    onItemClick = { item ->
                        navigateToItem(navController = navController, item = item)
                    },
                    navigateBack = {
                        navController.safePopBackStack()
                    },
                )
            }
            composable<CollectionRoute> { backStackEntry ->
                val route: CollectionRoute = backStackEntry.toRoute()
                CollectionScreen(
                    collectionId = UUID.fromString(route.collectionId),
                    collectionName = route.collectionName,
                    onItemClick = { item ->
                        navigateToItem(navController = navController, item = item)
                    },
                    navigateBack = {
                        navController.safePopBackStack()
                    },
                )
            }
            composable<FavoritesRoute> {
                FavoritesScreen(
                    onItemClick = { item ->
                        navigateToItem(navController = navController, item = item)
                    },
                    navigateBack = {
                        navController.safePopBackStack()
                    },
                )
            }
            composable<MovieRoute> { backStackEntry ->
                val route: MovieRoute = backStackEntry.toRoute()
                MovieScreen(
                    movieId = UUID.fromString(route.movieId),
                    navigateBack = {
                        navController.safePopBackStack()
                    },
                    navigateToPerson = { personId ->
                        navController.safeNavigate(PersonRoute(personId.toString()))
                    },
                )
            }
            composable<ShowRoute> { backStackEntry ->
                val route: ShowRoute = backStackEntry.toRoute()
                ShowScreen(
                    showId = UUID.fromString(route.showId),
                    navigateBack = {
                        navController.safePopBackStack()
                    },
                    navigateToItem = { item ->
                        navigateToItem(navController = navController, item = item)
                    },
                    navigateToPerson = { personId ->
                        navController.safeNavigate(PersonRoute(personId.toString()))
                    },
                )
            }
            composable<SeasonRoute> { backStackEntry ->
                val route: SeasonRoute = backStackEntry.toRoute()
                SeasonScreen(
                    seasonId = UUID.fromString(route.seasonId),
                    navigateBack = {
                        navController.safePopBackStack()
                    },
                    navigateToItem = { item ->
                        navigateToItem(navController = navController, item = item)
                    },
                )
            }
            composable<EpisodeRoute> { backStackEntry ->
                val route: EpisodeRoute = backStackEntry.toRoute()
                EpisodeScreen(
                    episodeId = UUID.fromString(route.episodeId),
                    navigateBack = {
                        navController.safePopBackStack()
                    },
                    navigateToPerson = { personId ->
                        navController.safeNavigate(PersonRoute(personId.toString()))
                    },
                )
            }
            composable<PersonRoute> { backStackEntry ->
                val route: PersonRoute = backStackEntry.toRoute()
                PersonScreen(
                    personId = UUID.fromString(route.personId),
                    navigateBack = {
                        navController.safePopBackStack()
                    },
                    navigateToItem = { item ->
                        navigateToItem(navController = navController, item = item)
                    },
                )
            }
            composable<SettingsRoute> { backStackEntry ->
                val route: SettingsRoute = backStackEntry.toRoute()
                SettingsScreen(
                    indexes = route.indexes,
                    navigateToSettings = { indexes ->
                        navController.safeNavigate(SettingsRoute(indexes = indexes))
                    },
                    navigateToServers = {
                        navController.safeNavigate(ServersRoute)
                    },
                    navigateToUsers = {
                        navController.safeNavigate(UsersRoute)
                    },
                    navigateToAbout = {
                        navController.safeNavigate(AboutRoute)
                    },
                    navigateBack = {
                        navController.safePopBackStack()
                    },
                )
            }
            composable<AboutRoute> {
                AboutScreen(
                    navigateBack = {
                        navController.safePopBackStack()
                    },
                )
            }
        }
    }
}

fun navigateToItem(navController: NavHostController, item: FindroidItem) {
    when (item) {
    is FindroidBoxSet -> navController.safeNavigate(CollectionRoute(collectionId = item.id.toString(), collectionName = item.name, onePerGenre = false))
        is FindroidMovie -> navController.safeNavigate(MovieRoute(movieId = item.id.toString()))
        is FindroidShow -> navController.safeNavigate(ShowRoute(showId = item.id.toString()))
        is FindroidSeason -> navController.safeNavigate(SeasonRoute(seasonId = item.id.toString()))
        is FindroidEpisode -> navController.safeNavigate(EpisodeRoute(episodeId = item.id.toString()))
        is FindroidCollection -> navController.safeNavigate(LibraryRoute(libraryId = item.id.toString(), libraryName = item.name, libraryType = item.type))
        else -> Unit
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
