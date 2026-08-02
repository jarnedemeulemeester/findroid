package dev.jdtech.jellyfin

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.adaptive.navigationsuite.rememberNavigationSuiteScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
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
import dev.jdtech.jellyfin.models.FindroidFolder
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.FindroidMovie
import dev.jdtech.jellyfin.models.FindroidSeason
import dev.jdtech.jellyfin.models.FindroidShow
import dev.jdtech.jellyfin.player.cast.models.CastConnectionState
import dev.jdtech.jellyfin.player.cast.presentation.CastSessionViewModel
import dev.jdtech.jellyfin.presentation.cast.CastExpandedPlayer
import dev.jdtech.jellyfin.presentation.cast.CastMiniPlayer
import dev.jdtech.jellyfin.presentation.cast.components.CastBottomSheet
import dev.jdtech.jellyfin.presentation.cast.components.CastButton
import dev.jdtech.jellyfin.presentation.film.CollectionScreen
import dev.jdtech.jellyfin.presentation.film.DownloadsScreen
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
import dev.jdtech.jellyfin.presentation.settings.SettingsFileEditScreen
import dev.jdtech.jellyfin.presentation.settings.SettingsScreen
import dev.jdtech.jellyfin.presentation.setup.addresses.ServerAddressesScreen
import dev.jdtech.jellyfin.presentation.setup.addserver.AddServerScreen
import dev.jdtech.jellyfin.presentation.setup.login.LoginScreen
import dev.jdtech.jellyfin.presentation.setup.servers.ServersScreen
import dev.jdtech.jellyfin.presentation.setup.users.UsersScreen
import dev.jdtech.jellyfin.presentation.setup.welcome.WelcomeScreen
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.presentation.utils.LocalOfflineMode
import dev.jdtech.jellyfin.presentation.utils.rememberSafePadding
import kotlinx.serialization.Serializable
import java.util.UUID
import dev.jdtech.jellyfin.core.R as CoreR

@Serializable
data object WelcomeRoute

@Serializable
data object ServersRoute

@Serializable
data object AddServerRoute

@Serializable
data class ServerAddressesRoute(val serverId: String)

@Serializable
data object UsersRoute

@Serializable
data class LoginRoute(val username: String? = null)

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
data class CollectionRoute(val collectionId: String, val collectionName: String)

@Serializable
data object FavoritesRoute

@Serializable
data class MovieRoute(val movieId: String)

@Serializable
data class ShowRoute(val showId: String)

@Serializable
data class EpisodeRoute(val episodeId: String)

@Serializable
data class SeasonRoute(val seasonId: String)

@Serializable
data class PersonRoute(val personId: String)

@Serializable
data class SettingsRoute(val indexes: IntArray)

@Serializable
data class SettingsFileEditRoute(
    val filePath: String,
)

@Serializable
data object AboutRoute

data class TabBarItem(
    @param:StringRes val title: Int,
    @param:DrawableRes val icon: Int,
    val route: Any,
    val enabled: Boolean = true,
)

val homeTab =
    TabBarItem(title = CoreR.string.title_home, icon = CoreR.drawable.ic_home, route = HomeRoute)
val mediaTab =
    TabBarItem(
        title = CoreR.string.title_media,
        icon = CoreR.drawable.ic_library,
        route = MediaRoute,
    )
val downloadsTab =
    TabBarItem(
        title = CoreR.string.title_download,
        icon = CoreR.drawable.ic_download,
        route = DownloadsRoute,
    )

val LocalCastPlayerHeight = compositionLocalOf { 0.dp }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationRoot(
    navController: NavHostController,
    hasServers: Boolean,
    hasCurrentServer: Boolean,
    hasCurrentUser: Boolean,
) {
    val context = LocalContext.current
    val castSessionViewModel: CastSessionViewModel = hiltViewModel(context as ViewModelStoreOwner)
    val isOfflineMode = LocalOfflineMode.current

    val startDestination =
        when {
            hasServers && hasCurrentServer && hasCurrentUser -> HomeRoute
            hasServers && hasCurrentServer -> UsersRoute
            hasServers -> ServersRoute
            else -> WelcomeRoute
        }

    val navigationItems =
        when (isOfflineMode) {
            false -> listOf(homeTab, mediaTab, downloadsTab)
            true -> listOf(homeTab, downloadsTab)
        }

    val navBackStackEntry by navController.currentBackStackEntryAsState()

    var searchExpanded by remember { mutableStateOf(false) }

    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = navigationItems.any {
        currentDestination?.hasRoute(it.route::class) == true
    } && !searchExpanded

    val safePadding = rememberSafePadding(
        handleStartInsets = false,
        handleBottomInsets = !showBottomBar
    )

    var castExpanded by remember { mutableStateOf(true) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (consumed.y < -1f) {
                    castExpanded = false // Scrolling down -> Collapse
                } else if (consumed.y > 1f) {
                    castExpanded = true // Scrolling up -> Expand
                }
                return Offset.Zero
            }
        }
    }

    val castRoutes = listOf(
        HomeRoute::class,
        ShowRoute::class,
        MovieRoute::class,
        MediaRoute::class,
        LibraryRoute::class,
        EpisodeRoute::class,
        SeasonRoute::class,
        FavoritesRoute::class,
        CollectionRoute::class,
    )

    val connectionState by castSessionViewModel.connectionState.collectAsStateWithLifecycle()
    val showCastButton =
        castRoutes.any { currentDestination?.hasRoute(it) == true } && !searchExpanded && !isOfflineMode && castSessionViewModel.sessionManager.isSupported
    var showCastSheet by remember { mutableStateOf(false) }
    var showCastExpandedPlayer by remember { mutableStateOf(false) }

    LaunchedEffect(connectionState) {
        if (connectionState != CastConnectionState.CONNECTED) {
            showCastExpandedPlayer = false
        }
    }

    val navigationSuiteScaffoldState = rememberNavigationSuiteScaffoldState()

    LaunchedEffect(showBottomBar) {
        if (showBottomBar) {
            navigationSuiteScaffoldState.show()
        } else {
            navigationSuiteScaffoldState.hide()
        }
    }

    val windowAdaptiveInfo = currentWindowAdaptiveInfo()
    val isExpandedScreen = windowAdaptiveInfo.windowSizeClass.isWidthAtLeastBreakpoint(
        WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND
    )
    val isMediumScreen = windowAdaptiveInfo.windowSizeClass.isWidthAtLeastBreakpoint(
        WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND
    )
    val customNavSuiteType =
        with(windowAdaptiveInfo) {
            if (
                windowSizeClass.isWidthAtLeastBreakpoint(
                    WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND
                )
            ) {
                NavigationSuiteType.NavigationRail
            } else {
                NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(this)
            }
        }

    val density = LocalDensity.current
    var castPlayerHeight by remember { mutableStateOf(MaterialTheme.spacings.default) }

    val showCastMiniPlayer by remember(
        showCastButton,
        connectionState,
        showCastExpandedPlayer,
        isExpandedScreen
    ) {
        derivedStateOf {
            val connected = showCastButton && connectionState == CastConnectionState.CONNECTED
            if (isExpandedScreen) {
                connected && !showCastExpandedPlayer
            } else {
                connected
            }
        }
    }

    LaunchedEffect(showCastMiniPlayer) {
        if (!showCastMiniPlayer) castPlayerHeight = MaterialTheme.spacings.default
    }

    CompositionLocalProvider(LocalCastPlayerHeight provides castPlayerHeight) {
        NavigationSuiteScaffold(
            navigationSuiteItems = {
                navigationItems.forEach { item ->
                    item(
                        selected = currentDestination?.hasRoute(item.route::class) == true,
                        onClick = {
                            if (
                                item.route is MediaRoute &&
                                currentDestination?.hasRoute<MediaRoute>() == true
                            ) {
                                searchExpanded = true
                            }

                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
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
                        label = { Text(text = stringResource(item.title)) },
                    )
                }
            },
            layoutType = customNavSuiteType,
            state = navigationSuiteScaffoldState,
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .nestedScroll(nestedScrollConnection)
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = startDestination,
                        enterTransition = { fadeIn(tween(300)) },
                        exitTransition = { fadeOut(tween(300)) },
                    ) {
                        composable<WelcomeRoute> {
                            WelcomeScreen(onContinueClick = {
                                navController.safeNavigate(
                                    ServersRoute
                                )
                            })
                        }
                        composable<ServersRoute> {
                            ServersScreen(
                                navigateToUsers = { navController.safeNavigate(UsersRoute) },
                                navigateToAddresses = { serverId ->
                                    navController.safeNavigate(ServerAddressesRoute(serverId))
                                },
                                onAddClick = { navController.safeNavigate(AddServerRoute) },
                                onBackClick = { navController.safePopBackStack() },
                                showBack = navController.previousBackStackEntry != null,
                            )
                        }
                        composable<AddServerRoute> {
                            AddServerScreen(
                                onSuccess = { navController.safeNavigate(UsersRoute) },
                                onBackClick = { navController.safePopBackStack() },
                            )
                        }
                        composable<ServerAddressesRoute> { backStackEntry ->
                            val route: ServerAddressesRoute = backStackEntry.toRoute()
                            ServerAddressesScreen(
                                serverId = route.serverId,
                                navigateBack = { navController.safePopBackStack() },
                            )
                        }
                        composable<UsersRoute> {
                            UsersScreen(
                                navigateToHome = { navigateHome(navController) },
                                onChangeServerClick = {
                                    navController.safeNavigate(ServersRoute) {
                                        popUpTo(ServersRoute) { inclusive = false }
                                        launchSingleTop = true
                                    }
                                },
                                onAddClick = { navController.safeNavigate(LoginRoute()) },
                                onBackClick = { navController.safePopBackStack() },
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
                                        popUpTo(ServersRoute) { inclusive = false }
                                        launchSingleTop = true
                                    }
                                },
                                onBackClick = { navController.safePopBackStack() },
                                prefilledUsername = route.username,
                            )
                        }
                        composable<HomeRoute> {
                            HomeScreen(
                                onLibraryClick = {
                                    navController.safeNavigate(
                                        LibraryRoute(
                                            libraryId = it.id.toString(),
                                            libraryName = it.name,
                                            libraryType = it.type,
                                        )
                                    )
                                },
                                onSearchClick = {
                                    searchExpanded = true
                                    navController.safeNavigate(MediaRoute) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                onSettingsClick = {
                                    navController.safeNavigate(
                                        SettingsRoute(indexes = intArrayOf(CoreR.string.title_settings))
                                    )
                                },
                                onManageServers = { navController.safeNavigate(ServersRoute) },
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
                                onFavoritesClick = { navController.safeNavigate(FavoritesRoute) },
                                searchExpanded = searchExpanded,
                                onSearchExpand = { searchExpanded = it },
                            )
                        }
                        composable<DownloadsRoute> {
                            DownloadsScreen(
                                onItemClick = { item ->
                                    navigateToItem(navController = navController, item = item)
                                }
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
                                navigateBack = { navController.safePopBackStack() },
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
                                navigateBack = { navController.safePopBackStack() },
                            )
                        }
                        composable<FavoritesRoute> {
                            FavoritesScreen(
                                onItemClick = { item ->
                                    navigateToItem(navController = navController, item = item)
                                },
                                navigateBack = { navController.safePopBackStack() },
                            )
                        }
                        composable<MovieRoute> { backStackEntry ->
                            val route: MovieRoute = backStackEntry.toRoute()
                            MovieScreen(
                                movieId = UUID.fromString(route.movieId),
                                navigateBack = { navController.safePopBackStack() },
                                navigateHome = { navigateHome(navController) },
                                navigateToPerson = { personId ->
                                    navController.safeNavigate(PersonRoute(personId.toString()))
                                },
                            )
                        }
                        composable<ShowRoute> { backStackEntry ->
                            val route: ShowRoute = backStackEntry.toRoute()
                            ShowScreen(
                                showId = UUID.fromString(route.showId),
                                navigateBack = { navController.safePopBackStack() },
                                navigateHome = { navigateHome(navController) },
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
                                navigateBack = { navController.safePopBackStack() },
                                navigateHome = { navigateHome(navController) },
                                navigateToItem = { item ->
                                    navigateToItem(navController = navController, item = item)
                                },
                                navigateToSeries = { seriesId ->
                                    navController.safeNavigate(ShowRoute(showId = seriesId.toString())) {
                                        popUpTo(ShowRoute(showId = seriesId.toString()))
                                        launchSingleTop = true
                                    }
                                },
                            )
                        }
                        composable<EpisodeRoute> { backStackEntry ->
                            val route: EpisodeRoute = backStackEntry.toRoute()
                            EpisodeScreen(
                                episodeId = UUID.fromString(route.episodeId),
                                navigateBack = { navController.safePopBackStack() },
                                navigateHome = { navigateHome(navController) },
                                navigateToPerson = { personId ->
                                    navController.safeNavigate(PersonRoute(personId.toString()))
                                },
                                navigateToSeason = { seasonId ->
                                    navController.safeNavigate(SeasonRoute(seasonId = seasonId.toString())) {
                                        popUpTo(SeasonRoute(seasonId = seasonId.toString()))
                                        launchSingleTop = true
                                    }
                                },
                            )
                        }
                        composable<PersonRoute> { backStackEntry ->
                            val route: PersonRoute = backStackEntry.toRoute()
                            PersonScreen(
                                personId = UUID.fromString(route.personId),
                                navigateBack = { navController.safePopBackStack() },
                                navigateHome = { navigateHome(navController) },
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
                                navigateToSettingsFileEdit = { filePath ->
                                    navController.safeNavigate(SettingsFileEditRoute(filePath = filePath))
                                },
                                navigateToServers = { navController.safeNavigate(ServersRoute) },
                                navigateToUsers = { navController.safeNavigate(UsersRoute) },
                                navigateToAbout = { navController.safeNavigate(AboutRoute) },
                                navigateBack = { navController.safePopBackStack() },
                            )
                        }
                        composable<SettingsFileEditRoute> { backStackEntry ->
                            val route: SettingsFileEditRoute = backStackEntry.toRoute()
                            SettingsFileEditScreen(
                                filePath = route.filePath,
                                navigateBack = { navController.safePopBackStack() })
                        }
                        composable<AboutRoute> {
                            AboutScreen(navigateBack = { navController.safePopBackStack() })
                        }
                    }

                    if (showCastButton && connectionState != CastConnectionState.CONNECTED) {
                        CastButton(
                            expanded = castExpanded,
                            onClick = { showCastSheet = true },
                            modifier = Modifier.align(Alignment.BottomEnd),
                            handleBottomInsets = !showBottomBar
                        )
                    }

                    // Cast Mini Player
                    if (showCastMiniPlayer) {
                        CastMiniPlayer(
                            onClick = { showCastExpandedPlayer = true },
                            modifier = Modifier
                                .align(if (isMediumScreen) Alignment.BottomEnd else Alignment.BottomCenter)
                                .onSizeChanged { size ->
                                    val measuredHeight = with(density) { size.height.toDp() }
                                    castPlayerHeight = if (!isMediumScreen) {
                                        measuredHeight - safePadding.bottom
                                    } else {
                                        MaterialTheme.spacings.default
                                    }
                                },
                            handleBottomInsets = !showBottomBar
                        )
                    }

                }

                if (showCastExpandedPlayer && isExpandedScreen) {
                    CastExpandedPlayer(
                        onDeviceClick = { showCastSheet = true },
                        onClose = { showCastExpandedPlayer = false }
                    )
                }
            }
        }
    }

    if (showCastSheet) {
        CastBottomSheet(
            onDismissRequest = { showCastSheet = false }
        )
    }

    // Cast Expanded Player
    if (showCastExpandedPlayer && !isExpandedScreen) {
        CastExpandedPlayer(
            onDeviceClick = { showCastSheet = true },
            onClose = { showCastExpandedPlayer = false }
        )
    }
}

private fun navigateHome(navController: NavHostController) {
    navController.safeNavigate(HomeRoute) {
        popUpTo(navController.graph.startDestinationId)
        launchSingleTop = true
    }
}

private fun navigateToItem(navController: NavHostController, item: FindroidItem) {
    when (item) {
        is FindroidBoxSet ->
            navController.safeNavigate(
                CollectionRoute(collectionId = item.id.toString(), collectionName = item.name)
            )

        is FindroidMovie -> navController.safeNavigate(MovieRoute(movieId = item.id.toString()))
        is FindroidShow -> navController.safeNavigate(ShowRoute(showId = item.id.toString()))
        is FindroidSeason -> navController.safeNavigate(SeasonRoute(seasonId = item.id.toString()))
        is FindroidEpisode ->
            navController.safeNavigate(EpisodeRoute(episodeId = item.id.toString()))

        is FindroidCollection ->
            navController.safeNavigate(
                LibraryRoute(
                    libraryId = item.id.toString(),
                    libraryName = item.name,
                    libraryType = item.type,
                )
            )

        is FindroidFolder ->
            navController.safeNavigate(
                LibraryRoute(
                    libraryId = item.id.toString(),
                    libraryName = item.name,
                    libraryType = CollectionType.Folders,
                )
            )

        else -> Unit
    }
}

private fun <T : Any> NavHostController.safeNavigate(
    route: T,
    navOptions: NavOptions? = null,
    navigatorExtras: Navigator.Extras? = null,
) {
    if (this.currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
        this.navigate(route, navOptions, navigatorExtras)
    }
}

private fun <T : Any> NavHostController.safeNavigate(
    route: T,
    builder: NavOptionsBuilder.() -> Unit,
) {
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
