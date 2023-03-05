package dev.jdtech.jellyfin.models

import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.repository.JellyfinRepository
import java.util.UUID
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind

interface FindroidItem {
    val id: UUID
    val name: String
    val originalTitle: String?
    val overview: String
    val played: Boolean
    val favorite: Boolean
    val canPlay: Boolean
    val canDownload: Boolean
    val sources: List<FindroidSource>
    val playbackPositionTicks: Long
    val unplayedItemCount: Int?
    val playedPercentage: Float?
}

suspend fun BaseItemDto.toJellyfinItem(
    jellyfinRepository: JellyfinRepository? = null,
    serverDatabase: ServerDatabaseDao? = null
): FindroidItem? {
    return when (type) {
        BaseItemKind.MOVIE -> toJellyfinMovieItem(jellyfinRepository, serverDatabase)
        BaseItemKind.EPISODE -> toJellyfinEpisodeItem(jellyfinRepository)
        BaseItemKind.SEASON -> toJellyfinSeasonItem()
        BaseItemKind.SERIES -> toJellyfinShowItem()
        BaseItemKind.BOX_SET -> toJellyfinBoxSet()
        else -> null
    }
}

fun FindroidItem.isDownloading(): Boolean {
    return sources.filter { it.type == JellyfinSourceType.LOCAL }.any { it.path.endsWith(".download")}
}

fun FindroidItem.isDownloaded(): Boolean {
    return sources.filter { it.type == JellyfinSourceType.LOCAL }.any { !it.path.endsWith(".download") }
}
