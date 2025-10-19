package dev.jdtech.jellyfin.models

import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.repository.JellyfinRepository
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import java.util.UUID

interface JellyCastItem {
    val id: UUID
    val name: String
    val originalTitle: String?
    val overview: String
    val played: Boolean
    val favorite: Boolean
    val canPlay: Boolean
    val canDownload: Boolean
    val sources: List<JellyCastSource>
    val runtimeTicks: Long
    val playbackPositionTicks: Long
    val unplayedItemCount: Int?
    val images: JellyCastImages
    val chapters: List<JellyCastChapter>
}

suspend fun BaseItemDto.toJellyCastItem(
    jellyfinRepository: JellyfinRepository,
    serverDatabase: ServerDatabaseDao? = null,
): JellyCastItem? {
    return when (type) {
        BaseItemKind.MOVIE -> toJellyCastMovie(jellyfinRepository, serverDatabase)
        BaseItemKind.EPISODE -> toJellyCastEpisode(jellyfinRepository)
        BaseItemKind.SEASON -> toJellyCastSeason(jellyfinRepository)
        BaseItemKind.SERIES -> toJellyCastShow(jellyfinRepository)
        BaseItemKind.BOX_SET -> toJellyCastBoxSet(jellyfinRepository)
        BaseItemKind.FOLDER -> toJellyCastFolder(jellyfinRepository)
        else -> null
    }
}

fun JellyCastItem.isDownloading(): Boolean {
    return sources.filter { it.type == JellyCastSourceType.LOCAL }.any { it.path.endsWith(".download") }
}

fun JellyCastItem.isDownloaded(): Boolean {
    return sources.filter { it.type == JellyCastSourceType.LOCAL }.any { !it.path.endsWith(".download") }
}
