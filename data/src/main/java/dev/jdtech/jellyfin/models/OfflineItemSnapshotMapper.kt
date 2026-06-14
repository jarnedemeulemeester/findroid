package dev.jdtech.jellyfin.models

import android.net.Uri
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.offline.download.OfflineItemKind
import dev.jdtech.jellyfin.offline.download.OfflineItemSnapshot
import java.util.UUID

fun FindroidItem.toOfflineItemSnapshot(
    packageId: String,
    serverId: String,
    nowMillis: Long,
): OfflineItemSnapshot? =
    when (this) {
        is FindroidMovie ->
            OfflineItemSnapshot(
                packageId = packageId,
                serverId = serverId,
                itemId = id.toString(),
                itemKind = OfflineItemKind.MOVIE,
                name = name,
                originalTitle = originalTitle,
                overview = overview,
                runtimeTicks = runtimeTicks,
                playbackPositionTicks = playbackPositionTicks,
                played = played,
                favorite = favorite,
                communityRating = communityRating,
                productionYear = productionYear,
                createdAtMillis = nowMillis,
                updatedAtMillis = nowMillis,
            )
        is FindroidEpisode ->
            OfflineItemSnapshot(
                packageId = packageId,
                serverId = serverId,
                itemId = id.toString(),
                itemKind = OfflineItemKind.EPISODE,
                name = name,
                originalTitle = originalTitle,
                overview = overview,
                runtimeTicks = runtimeTicks,
                playbackPositionTicks = playbackPositionTicks,
                played = played,
                favorite = favorite,
                seriesId = seriesId.toString(),
                seriesName = seriesName,
                seasonId = seasonId.toString(),
                seasonName = seasonName,
                indexNumber = indexNumber,
                indexNumberEnd = indexNumberEnd,
                parentIndexNumber = parentIndexNumber,
                communityRating = communityRating,
                createdAtMillis = nowMillis,
                updatedAtMillis = nowMillis,
            )
        else -> null
    }

fun OfflineItemSnapshotDto.toFindroidItem(
    database: ServerDatabaseDao,
    userId: UUID,
    localSource: FindroidSource,
): FindroidItem? {
    val itemUuid = itemId.toUuidOrNull() ?: return null
    val userData = database.getUserData(itemUuid, userId)
    val played = userData?.played ?: this.played
    val favorite = userData?.favorite ?: this.favorite
    val playbackPositionTicks = userData?.playbackPositionTicks ?: this.playbackPositionTicks
    return when (itemKind) {
        OfflineItemKind.MOVIE ->
            FindroidMovie(
                id = itemUuid,
                name = name,
                originalTitle = originalTitle,
                overview = overview,
                sources = listOf(localSource),
                played = played,
                favorite = favorite,
                canPlay = true,
                canDownload = false,
                runtimeTicks = runtimeTicks,
                playbackPositionTicks = playbackPositionTicks,
                premiereDate = null,
                people = emptyList(),
                genres = emptyList(),
                communityRating = communityRating,
                officialRating = null,
                status = "Downloaded",
                productionYear = productionYear,
                endDate = null,
                trailer = null,
                images = localFindroidImages(itemUuid),
                chapters = emptyList(),
                trickplayInfo = null,
            )
        OfflineItemKind.EPISODE -> {
            val seriesUuid = seriesId?.toUuidOrNull() ?: return null
            val seasonUuid = seasonId?.toUuidOrNull() ?: return null
            FindroidEpisode(
                id = itemUuid,
                name = name,
                originalTitle = originalTitle,
                overview = overview,
                indexNumber = indexNumber ?: 0,
                indexNumberEnd = indexNumberEnd,
                parentIndexNumber = parentIndexNumber ?: 0,
                sources = listOf(localSource),
                played = played,
                favorite = favorite,
                canPlay = true,
                canDownload = false,
                runtimeTicks = runtimeTicks,
                playbackPositionTicks = playbackPositionTicks,
                premiereDate = null,
                seriesId = seriesUuid,
                seriesName = seriesName.orEmpty(),
                seasonId = seasonUuid,
                seasonName = seasonName,
                communityRating = communityRating,
                people = emptyList(),
                images = localFindroidImages(itemUuid, seriesUuid),
                chapters = emptyList(),
                trickplayInfo = null,
            )
        }
    }
}

private fun localFindroidImages(itemId: UUID, seriesId: UUID? = null): FindroidImages =
    FindroidImages(
        primary = Uri.Builder().appendEncodedPath("images/$itemId/primary").build(),
        backdrop = Uri.Builder().appendEncodedPath("images/$itemId/backdrop").build(),
        logo = Uri.Builder().appendEncodedPath("images/$itemId/logo").build(),
        showPrimary = seriesId?.let { Uri.Builder().appendEncodedPath("images/$it/primary").build() },
        showBackdrop = seriesId?.let { Uri.Builder().appendEncodedPath("images/$it/backdrop").build() },
        showLogo = seriesId?.let { Uri.Builder().appendEncodedPath("images/$it/logo").build() },
    )

private fun String.toUuidOrNull(): UUID? =
    try {
        UUID.fromString(this)
    } catch (_: IllegalArgumentException) {
        null
    }
