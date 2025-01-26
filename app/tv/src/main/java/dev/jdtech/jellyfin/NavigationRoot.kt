package dev.jdtech.jellyfin

import android.os.Parcel
import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import dev.jdtech.jellyfin.models.CollectionType
import dev.jdtech.jellyfin.models.PlayerItem
import dev.jdtech.jellyfin.presentation.settings.SettingsScreen
import dev.jdtech.jellyfin.presentation.settings.SettingsSubScreen
import dev.jdtech.jellyfin.presentation.setup.addserver.AddServerScreen
import dev.jdtech.jellyfin.presentation.setup.login.LoginScreen
import dev.jdtech.jellyfin.presentation.setup.servers.ServersScreen
import dev.jdtech.jellyfin.presentation.setup.users.UsersScreen
import dev.jdtech.jellyfin.presentation.setup.welcome.WelcomeScreen
import dev.jdtech.jellyfin.ui.LibraryScreen
import dev.jdtech.jellyfin.ui.MainScreen
import dev.jdtech.jellyfin.ui.MovieScreen
import dev.jdtech.jellyfin.ui.PlayerScreen
import dev.jdtech.jellyfin.ui.SeasonScreen
import dev.jdtech.jellyfin.ui.ShowScreen
import dev.jdtech.jellyfin.utils.base64ToByteArray
import dev.jdtech.jellyfin.utils.toBase64Str
import kotlinx.parcelize.parcelableCreator
import kotlinx.serialization.Serializable
import java.util.UUID

inline fun <reified T : Parcelable> T.toBase64(): String {
    val parcel = Parcel.obtain()
    this.writeToParcel(parcel, 0)
    val bytearray = parcel.marshall()
    parcel.recycle()
    return bytearray.toBase64Str()
}

inline fun <reified T : Parcelable> String.base64ToParcelable(): T {
    val bytearray = this.base64ToByteArray()
    val parcel = Parcel.obtain()
    parcel.unmarshall(bytearray, 0, bytearray.size)
    parcel.setDataPosition(0)
    val item = parcelableCreator<T>().createFromParcel(parcel)
    return item
}

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
data object MainRoute

@Serializable
data class LibraryRoute(
    val libraryId: String,
    val libraryName: String,
    val libraryType: CollectionType,
)

@Serializable
data class MovieRoute(
    val itemId: String,
)

@Serializable
data class ShowRoute(
    val itemId: String,
)

@Serializable
data class SeasonRoute(
    val seasonId: String,
    val seriesId: String,
    val seasonName: String,
    val seriesName: String,
)

@Serializable
data class PlayerRoute(
    val items: Array<String>,
)

@Serializable
data object SettingsRoute

@Serializable
data class SettingsSubRoute(
    val indexes: IntArray,
)

@OptIn(ExperimentalStdlibApi::class)
@Composable
fun NavigationRoot(
    navController: NavHostController,
    hasServers: Boolean,
    hasCurrentServer: Boolean,
    hasCurrentUser: Boolean,
) {
    val startDestination = when {
        hasServers && hasCurrentServer && hasCurrentUser -> MainRoute
        hasServers && hasCurrentServer -> UsersRoute
        hasServers -> ServersRoute
        else -> WelcomeRoute
    }
    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable<WelcomeRoute> {
            WelcomeScreen(
                onContinueClick = {
                    navController.navigate(ServersRoute)
                },
            )
        }
        composable<ServersRoute> {
            ServersScreen(
                navigateToLogin = {
                    navController.navigate(LoginRoute)
                },
                navigateToUsers = {
                    navController.navigate(UsersRoute)
                },
                onAddClick = {
                    navController.navigate(AddServerRoute)
                },
            )
        }
        composable<AddServerRoute> {
            AddServerScreen(
                onSuccess = {
                    navController.navigate(LoginRoute)
                },
            )
        }
        composable<UsersRoute> {
            UsersScreen(
                navigateToHome = {
                    navController.navigate(MainRoute) {
                        popUpTo(startDestination) {
                            inclusive = true
                        }
                    }
                },
                onChangeServerClick = {
                    navController.navigate(ServersRoute) {
                        popUpTo(ServersRoute) {
                            inclusive = false
                        }
                        launchSingleTop = true
                    }
                },
                onAddClick = {
                    navController.navigate(LoginRoute)
                },
            )
        }
        composable<LoginRoute> {
            LoginScreen(
                onSuccess = {
                    navController.navigate(MainRoute) {
                        popUpTo(startDestination) {
                            inclusive = true
                        }
                    }
                },
                onChangeServerClick = {
                    navController.navigate(ServersRoute) {
                        popUpTo(ServersRoute) {
                            inclusive = false
                        }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable<MainRoute> {
            MainScreen(
                navigateToSettings = {
                    navController.navigate(SettingsRoute)
                },
                navigateToLibrary = { libraryId, libraryName, libraryType ->
                    navController.navigate(LibraryRoute(libraryId = libraryId.toString(), libraryName = libraryName, libraryType = libraryType))
                },
                navigateToMovie = { itemId ->
                    navController.navigate(MovieRoute(itemId.toString()))
                },
                navigateToShow = { itemId ->
                    navController.navigate(ShowRoute(itemId.toString()))
                },
                navigateToPlayer = { items ->
                    val mappedItems = items.map { it.toBase64() }.toTypedArray()
                    navController.navigate(PlayerRoute(mappedItems))
                },
            )
        }
        composable<LibraryRoute> { backStackEntry ->
            val route: LibraryRoute = backStackEntry.toRoute()
            LibraryScreen(
                libraryId = UUID.fromString(route.libraryId),
                libraryName = route.libraryName,
                libraryType = route.libraryType,
                navigateToLibrary = { libraryId, libraryName, libraryType ->
                    navController.navigate(LibraryRoute(libraryId = libraryId.toString(), libraryName = libraryName, libraryType = libraryType))
                },
                navigateToMovie = { itemId ->
                    navController.navigate(MovieRoute(itemId.toString()))
                },
                navigateToShow = { itemId ->
                    navController.navigate(ShowRoute(itemId.toString()))
                },
            )
        }
        composable<MovieRoute> { backStackEntry ->
            val route: MovieRoute = backStackEntry.toRoute()
            MovieScreen(
                itemId = UUID.fromString(route.itemId),
                navigateToPlayer = { items ->
                    val mappedItems = items.map { it.toBase64() }.toTypedArray()
                    navController.navigate(PlayerRoute(mappedItems))
                },
            )
        }
        composable<ShowRoute> { backStackEntry ->
            val route: ShowRoute = backStackEntry.toRoute()
            ShowScreen(
                itemId = UUID.fromString(route.itemId),
                navigateToSeason = { seriesId, seasonId, seriesName, seasonName ->
                    navController.navigate(SeasonRoute(seasonId = seasonId.toString(), seriesId = seriesId.toString(), seasonName = seasonName, seriesName = seriesName))
                },
                navigateToPlayer = { items ->
                    val mappedItems = items.map { it.toBase64() }.toTypedArray()
                    navController.navigate(PlayerRoute(mappedItems))
                },
            )
        }
        composable<SeasonRoute> { backStackEntry ->
            val route: SeasonRoute = backStackEntry.toRoute()
            SeasonScreen(
                seasonId = UUID.fromString(route.seasonId),
                seriesId = UUID.fromString(route.seriesId),
                seasonName = route.seasonName,
                seriesName = route.seriesName,
                navigateToPlayer = { items ->
                    val mappedItems = items.map { it.toBase64() }.toTypedArray()
                    navController.navigate(PlayerRoute(mappedItems))
                },
            )
        }
        composable<PlayerRoute> { backStackEntry ->
            val route: PlayerRoute = backStackEntry.toRoute()
            val items = route.items.map { it.base64ToParcelable<PlayerItem>() }.toTypedArray()
            PlayerScreen(
                items = items,
            )
        }
        composable<SettingsRoute> {
            SettingsScreen(
                navigateToUsers = {
                    navController.navigate(UsersRoute)
                },
                navigateToServers = {
                    navController.navigate(ServersRoute)
                },
                navigateToSubSettings = { indexes ->
                    navController.navigate(SettingsSubRoute(indexes = indexes))
                },
            )
        }
        composable<SettingsSubRoute> { backStackEntry ->
            val route: SettingsSubRoute = backStackEntry.toRoute()
            SettingsSubScreen(
                indexes = route.indexes,
                navigateToUsers = {
                    navController.navigate(UsersRoute)
                },
                navigateToServers = {
                    navController.navigate(ServersRoute)
                },
                navigateToSubSettings = { indexes ->
                    navController.navigate(SettingsSubRoute(indexes = indexes))
                },
            )
        }
    }
}
