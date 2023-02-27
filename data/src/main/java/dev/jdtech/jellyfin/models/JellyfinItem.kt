package dev.jdtech.jellyfin.models

import dev.jdtech.jellyfin.repository.JellyfinRepository
import java.util.UUID
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind

interface JellyfinItem {
    val id: UUID
    val name: String
    val originalTitle: String?
    val overview: String
    val played: Boolean
    val favorite: Boolean
    val canPlay: Boolean
    val canDownload: Boolean
    val sources: List<JellyfinSource>
    val playbackPositionTicks: Long
    val unplayedItemCount: Int?
    val playedPercentage: Float?
}

suspend fun BaseItemDto.toJellyfinItem(jellyfinRepository: JellyfinRepository? = null): JellyfinItem? {
    return when (type) {
        BaseItemKind.MOVIE -> toJellyfinMovieItem(jellyfinRepository)
        BaseItemKind.EPISODE -> toJellyfinEpisodeItem(jellyfinRepository)
        BaseItemKind.SEASON -> toJellyfinSeasonItem()
        BaseItemKind.SERIES -> toJellyfinShowItem()
        BaseItemKind.BOX_SET -> toJellyfinBoxSet()
        else -> null
    }
}

fun JellyfinItem.isDownloaded(): Boolean {
    return sources.any { it.type == JellyfinSourceType.LOCAL }
}