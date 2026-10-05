package dev.jdtech.jellyfin

import androidx.navigation3.runtime.NavKey
import dev.jdtech.jellyfin.models.CollectionType
import kotlinx.serialization.Serializable

@Serializable data object WelcomeRoute : NavKey

@Serializable data object ServersRoute : NavKey

@Serializable data object AddServerRoute : NavKey

@Serializable data object UsersRoute : NavKey

@Serializable data class LoginRoute(val username: String? = null) : NavKey

@Serializable data object MainRoute : NavKey

@Serializable
data class LibraryRoute(
    val libraryId: String,
    val libraryName: String,
    val libraryType: CollectionType,
) : NavKey

@Serializable data class MovieRoute(val itemId: String) : NavKey

@Serializable data class ShowRoute(val itemId: String) : NavKey

@Serializable data class SeasonRoute(val seasonId: String) : NavKey

@Serializable data class PlayerRoute(val itemId: String, val itemKind: String) : NavKey

@Serializable data object SettingsRoute : NavKey

@Serializable data class SettingsSubRoute(val indexes: List<Int>) : NavKey
