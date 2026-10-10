package dev.jdtech.jellyfin

import androidx.navigation3.runtime.NavKey
import dev.jdtech.jellyfin.models.CollectionType
import kotlinx.serialization.Serializable

@Serializable data object WelcomeRoute : NavKey

@Serializable data object ServersRoute : NavKey

@Serializable data object AddServerRoute : NavKey

@Serializable data class ServerAddressesRoute(val serverId: String) : NavKey

@Serializable data object UsersRoute : NavKey

@Serializable data class LoginRoute(val username: String? = null) : NavKey

@Serializable data object HomeRoute : NavKey

@Serializable data object MediaRoute : NavKey

@Serializable data object DownloadsRoute : NavKey

@Serializable
data class LibraryRoute(
    val libraryId: String,
    val libraryName: String,
    val libraryType: CollectionType,
) : NavKey

@Serializable
data class CollectionRoute(val collectionId: String, val collectionName: String) : NavKey

@Serializable data object FavoritesRoute : NavKey

@Serializable data class MovieRoute(val movieId: String) : NavKey

@Serializable data class ShowRoute(val showId: String) : NavKey

@Serializable data class EpisodeRoute(val episodeId: String) : NavKey

@Serializable data class SeasonRoute(val seasonId: String) : NavKey

@Serializable data class PersonRoute(val personId: String) : NavKey

@Serializable data class SettingsRoute(val indexes: List<Int>) : NavKey

@Serializable data class SettingsFileEditRoute(val filePath: String) : NavKey

@Serializable data object AboutRoute : NavKey
