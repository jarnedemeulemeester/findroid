package dev.jdtech.jellyfin.car

import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.FindroidMovie
import dev.jdtech.jellyfin.models.FindroidSeason
import dev.jdtech.jellyfin.models.FindroidShow
import dev.jdtech.jellyfin.offline.download.OfflineAssetKind
import dev.jdtech.jellyfin.offline.download.OfflineAssetStatus
import dev.jdtech.jellyfin.offline.download.OfflineItemKind
import dev.jdtech.jellyfin.offline.download.OfflineItemSnapshot
import dev.jdtech.jellyfin.offline.download.OfflinePackageManifest
import dev.jdtech.jellyfin.offline.download.OfflineStorageScope
import java.io.File
import org.jellyfin.sdk.model.api.BaseItemKind

enum class FindroidCarCatalogItemKind {
    MOVIE,
    SERIES,
    SEASON,
    EPISODE,
}

data class FindroidCarCatalogItem(
    val packageId: String,
    val itemId: String,
    val itemKind: FindroidCarCatalogItemKind,
    val playerItemKind: String,
    val seriesId: String?,
    val seriesName: String?,
    val seasonId: String?,
    val seasonName: String?,
    val indexNumber: Int?,
    val parentIndexNumber: Int?,
    val title: String,
    val subtitle: String,
    val runtimeText: String,
    val runtimeTicks: Long,
    val artworkPaths: List<String>,
    val videoPath: String?,
    val streamUrl: String?,
    val played: Boolean,
    val favorite: Boolean,
    val playbackPositionTicks: Long,
    val unplayedItemCount: Int?,
)

fun OfflineItemSnapshot.toFindroidCarCatalogItem(
    packageManifest: OfflinePackageManifest?
): FindroidCarCatalogItem {
    val title =
        when (itemKind) {
            OfflineItemKind.MOVIE -> name
            OfflineItemKind.EPISODE -> episodeTitle()
        }
    val subtitle =
        when (itemKind) {
            OfflineItemKind.MOVIE -> productionYear?.toString().orEmpty()
            OfflineItemKind.EPISODE ->
                listOfNotNull(seriesName, parentIndexNumber?.let { "Season $it" })
                    .joinToString(" / ")
        }
    return FindroidCarCatalogItem(
        packageId = packageId,
        itemId = itemId,
        itemKind =
            when (itemKind) {
                OfflineItemKind.MOVIE -> FindroidCarCatalogItemKind.MOVIE
                OfflineItemKind.EPISODE -> FindroidCarCatalogItemKind.EPISODE
            },
        playerItemKind =
            when (itemKind) {
                OfflineItemKind.MOVIE -> BaseItemKind.MOVIE.serialName
                OfflineItemKind.EPISODE -> BaseItemKind.EPISODE.serialName
            },
        seriesId = seriesId,
        seriesName = seriesName,
        seasonId = seasonId,
        seasonName = seasonName,
        indexNumber = indexNumber,
        parentIndexNumber = parentIndexNumber,
        title = title,
        subtitle = subtitle,
        runtimeText = runtimeText(),
        runtimeTicks = runtimeTicks,
        artworkPaths = packageManifest?.readyArtworkPaths().orEmpty(),
        videoPath = packageManifest?.readyPublicVideoPath(),
        streamUrl = null,
        played = played,
        favorite = favorite,
        playbackPositionTicks = playbackPositionTicks,
        unplayedItemCount = null,
    )
}

fun FindroidItem.toFindroidCarCatalogItem(filesDir: File? = null): FindroidCarCatalogItem? {
    val catalogData =
        when (this) {
            is FindroidMovie ->
                CatalogData(
                    itemKind = FindroidCarCatalogItemKind.MOVIE,
                    playerItemKind = BaseItemKind.MOVIE.serialName,
                    title = name,
                    subtitle = productionYear?.toString().orEmpty(),
                )
            is FindroidShow ->
                CatalogData(
                    itemKind = FindroidCarCatalogItemKind.SERIES,
                    playerItemKind = BaseItemKind.SERIES.serialName,
                    title = name,
                    subtitle = productionYear?.toString().orEmpty(),
                    seriesId = id.toString(),
                    seriesName = name,
                )
            is FindroidSeason ->
                CatalogData(
                    itemKind = FindroidCarCatalogItemKind.SEASON,
                    playerItemKind = BaseItemKind.SEASON.serialName,
                    title = seasonTitle(),
                    subtitle = seriesName,
                    seriesId = seriesId.toString(),
                    seriesName = seriesName,
                    seasonId = id.toString(),
                    seasonName = name,
                    parentIndexNumber = indexNumber.takeIf { it > 0 },
                )
            is FindroidEpisode ->
                CatalogData(
                    itemKind = FindroidCarCatalogItemKind.EPISODE,
                    playerItemKind = BaseItemKind.EPISODE.serialName,
                    title = onlineEpisodeTitle(),
                    subtitle = listOf(seriesName, "Season $parentIndexNumber").joinToString(" / "),
                    seriesId = seriesId.toString(),
                    seriesName = seriesName,
                    seasonId = seasonId.toString(),
                    seasonName = seasonName,
                    indexNumber = indexNumber.takeIf { it > 0 },
                    parentIndexNumber = parentIndexNumber.takeIf { it > 0 },
                )
            else -> return null
        }

    return FindroidCarCatalogItem(
        packageId = "online:$id",
        itemId = id.toString(),
        itemKind = catalogData.itemKind,
        playerItemKind = catalogData.playerItemKind,
        seriesId = catalogData.seriesId,
        seriesName = catalogData.seriesName,
        seasonId = catalogData.seasonId,
        seasonName = catalogData.seasonName,
        indexNumber = catalogData.indexNumber,
        parentIndexNumber = catalogData.parentIndexNumber,
        title = catalogData.title,
        subtitle = catalogData.subtitle,
        runtimeText = runtimeText(),
        runtimeTicks = runtimeTicks,
        artworkPaths = cachedArtworkPaths(filesDir),
        videoPath = null,
        streamUrl = null,
        played = played,
        favorite = favorite,
        playbackPositionTicks = playbackPositionTicks,
        unplayedItemCount = unplayedItemCount,
    )
}

fun FindroidItem.toFindroidCarCatalogItemWithCachedArtwork(
    filesDir: File,
    accessToken: String?,
): FindroidCarCatalogItem? {
    val item = toFindroidCarCatalogItem(filesDir) ?: return null
    val artworkPaths = FindroidCarOnlineArtworkCache.ensureCached(this, filesDir, accessToken)
    return item.copy(artworkPaths = artworkPaths.ifEmpty { item.artworkPaths })
}

internal fun FindroidCarCatalogItem.watchStatusText(): String =
    when {
        played -> "Watched"
        unplayedItemCount == 0 -> "Watched"
        unplayedItemCount != null -> "$unplayedItemCount unwatched"
        playbackPositionTicks > 0L -> "In progress"
        else -> ""
    }

internal fun List<FindroidCarCatalogItem>.offlineSeriesItems(): List<FindroidCarCatalogItem> =
    offlineEpisodes()
        .filter { !it.seriesKey.isNullOrBlank() }
        .groupBy { it.seriesKey.orEmpty() }
        .values
        .mapNotNull { episodes ->
            val first = episodes.sortedWith(offlineEpisodeComparator).firstOrNull() ?: return@mapNotNull null
            val seasonCount = episodes.mapNotNull { it.seasonKey }.distinct().size
            first.copy(
                packageId = "offline-series:${first.seriesKey}",
                itemId = first.seriesId ?: first.seriesKey.orEmpty(),
                itemKind = FindroidCarCatalogItemKind.SERIES,
                playerItemKind = BaseItemKind.SERIES.serialName,
                seasonId = null,
                seasonName = null,
                indexNumber = null,
                parentIndexNumber = null,
                title = first.seriesName ?: first.subtitle.substringBefore(" / "),
                subtitle =
                    when {
                        seasonCount > 1 -> "$seasonCount seasons"
                        seasonCount == 1 -> "1 season"
                        else -> ""
                    },
                runtimeText = episodeCountText(episodes.size),
                artworkPaths = episodes.firstNotEmptyArtworkPaths(),
                videoPath = null,
                streamUrl = null,
                unplayedItemCount = episodes.count { !it.played },
            )
        }

internal fun List<FindroidCarCatalogItem>.offlineSeasonItems(
    series: FindroidCarCatalogItem
): List<FindroidCarCatalogItem> =
    offlineEpisodes()
        .filter { it.seriesKey == series.seriesKey }
        .groupBy { it.seasonKey ?: "season:${it.parentIndexNumber ?: 0}" }
        .values
        .mapNotNull { episodes ->
            val first = episodes.sortedWith(offlineEpisodeComparator).firstOrNull() ?: return@mapNotNull null
            first.copy(
                packageId = "offline-season:${series.seriesKey}:${first.seasonKey}",
                itemId = first.seasonId ?: first.seasonKey.orEmpty(),
                itemKind = FindroidCarCatalogItemKind.SEASON,
                playerItemKind = BaseItemKind.SEASON.serialName,
                indexNumber = null,
                title = first.seasonDisplayTitle(),
                subtitle = first.seriesName ?: series.title,
                runtimeText = episodeCountText(episodes.size),
                artworkPaths = episodes.firstNotEmptyArtworkPaths(),
                videoPath = null,
                streamUrl = null,
                unplayedItemCount = episodes.count { !it.played },
            )
        }
        .sortedWith(compareBy<FindroidCarCatalogItem> { it.parentIndexNumber ?: Int.MAX_VALUE }
            .thenBy { it.title })

internal fun List<FindroidCarCatalogItem>.offlineEpisodeItems(
    series: FindroidCarCatalogItem,
    season: FindroidCarCatalogItem,
): List<FindroidCarCatalogItem> =
    offlineEpisodes()
        .filter { it.seriesKey == series.seriesKey && it.seasonKey == season.seasonKey }
        .sortedWith(offlineEpisodeComparator)

internal fun List<FindroidCarCatalogItem>.offlineEpisodes(): List<FindroidCarCatalogItem> =
    filter { it.itemKind == FindroidCarCatalogItemKind.EPISODE && !it.videoPath.isNullOrBlank() }

internal val FindroidCarCatalogItem.seriesKey: String?
    get() = seriesId ?: seriesName?.takeIf { it.isNotBlank() }

internal val FindroidCarCatalogItem.seasonKey: String?
    get() =
        seasonId
            ?: parentIndexNumber?.let { "season:$it" }
            ?: seasonName?.takeIf { it.isNotBlank() }

internal val offlineEpisodeComparator: Comparator<FindroidCarCatalogItem> =
    compareBy<FindroidCarCatalogItem>(
            { it.parentIndexNumber ?: Int.MAX_VALUE },
            { it.indexNumber ?: Int.MAX_VALUE },
            { it.title },
        )

private fun FindroidCarCatalogItem.seasonDisplayTitle(): String =
    seasonName?.takeIf { it.isNotBlank() }
        ?: parentIndexNumber?.let { "Season $it" }
        ?: "Season"

private fun episodeCountText(count: Int): String =
    when (count) {
        1 -> "1 episode"
        else -> "$count episodes"
    }

private fun List<FindroidCarCatalogItem>.firstNotEmptyArtworkPaths(): List<String> =
    firstOrNull { it.artworkPaths.isNotEmpty() }?.artworkPaths.orEmpty()

private fun OfflinePackageManifest.readyPublicVideoPath(): String? =
    assets
        .firstOrNull {
            it.kind == OfflineAssetKind.VIDEO &&
                it.storageScope == OfflineStorageScope.PUBLIC_MEDIA &&
                it.status == OfflineAssetStatus.READY &&
                !it.finalPath.isNullOrBlank()
        }
        ?.finalPath

private fun OfflinePackageManifest.readyArtworkPaths(): List<String> {
    val preferredKinds =
        listOf(
            OfflineAssetKind.PUBLIC_FOLDER_POSTER,
            OfflineAssetKind.POSTER_PRIMARY,
            OfflineAssetKind.SERIES_PRIMARY,
            OfflineAssetKind.SEASON_PRIMARY,
            OfflineAssetKind.BACKDROP,
        )
    return preferredKinds
        .flatMap { kind ->
            assets
                .filter {
                    it.kind == kind &&
                        it.status == OfflineAssetStatus.READY &&
                        !it.finalPath.isNullOrBlank()
                }
                .mapNotNull { it.finalPath }
        }
        .distinct()
}

private fun OfflineItemSnapshot.episodeTitle(): String {
    val index = indexNumber ?: return name
    return "E${index.toString().padStart(2, '0')} - $name"
}

private fun OfflineItemSnapshot.runtimeText(): String {
    val minutes = runtimeTicks / 10_000_000L / 60L
    return if (minutes > 0) "$minutes min" else ""
}

private fun FindroidEpisode.onlineEpisodeTitle(): String {
    if (indexNumber <= 0) return name
    return "E${indexNumber.toString().padStart(2, '0')} - $name"
}

private fun FindroidSeason.seasonTitle(): String =
    name.ifBlank {
        if (indexNumber > 0) "Season $indexNumber" else "Season"
    }

private fun FindroidItem.runtimeText(): String {
    val minutes = runtimeTicks / 10_000_000L / 60L
    return if (minutes > 0) "$minutes min" else ""
}

private fun FindroidItem.cachedArtworkPaths(filesDir: File?): List<String> {
    if (filesDir == null) return emptyList()
    val carArtwork = FindroidCarOnlineArtworkCache.cachedPaths(this, filesDir)
    if (carArtwork.isNotEmpty()) return carArtwork
    val ids =
        buildList {
            add(id)
            if (this@cachedArtworkPaths is FindroidEpisode) add(seriesId)
            if (this@cachedArtworkPaths is FindroidSeason) add(seriesId)
        }
    return ids
        .flatMap { itemId ->
            listOf(
                File(filesDir, "images/$itemId/primary"),
                File(filesDir, "images/$itemId/backdrop"),
            )
        }
        .filter { it.isFile }
        .map { it.absolutePath }
}

private data class CatalogData(
    val itemKind: FindroidCarCatalogItemKind,
    val playerItemKind: String,
    val title: String,
    val subtitle: String,
    val seriesId: String? = null,
    val seriesName: String? = null,
    val seasonId: String? = null,
    val seasonName: String? = null,
    val indexNumber: Int? = null,
    val parentIndexNumber: Int? = null,
)
