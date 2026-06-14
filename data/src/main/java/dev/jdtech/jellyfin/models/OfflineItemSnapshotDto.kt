package dev.jdtech.jellyfin.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.jdtech.jellyfin.offline.download.OfflineItemKind
import dev.jdtech.jellyfin.offline.download.OfflineItemSnapshot

@Entity(
    tableName = "offlineItemSnapshots",
    foreignKeys =
        [
            ForeignKey(
                entity = OfflinePackageDto::class,
                parentColumns = ["packageId"],
                childColumns = ["packageId"],
                onDelete = ForeignKey.CASCADE,
            )
        ],
    indices = [Index("serverId"), Index("itemId"), Index("itemKind")],
)
data class OfflineItemSnapshotDto(
    @PrimaryKey val packageId: String,
    val serverId: String,
    val itemId: String,
    val itemKind: OfflineItemKind,
    val name: String,
    val originalTitle: String?,
    val overview: String,
    val runtimeTicks: Long,
    val playbackPositionTicks: Long,
    val played: Boolean,
    val favorite: Boolean,
    val seriesId: String?,
    val seriesName: String?,
    val seasonId: String?,
    val seasonName: String?,
    val indexNumber: Int?,
    val indexNumberEnd: Int?,
    val parentIndexNumber: Int?,
    val communityRating: Float?,
    val productionYear: Int?,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

fun OfflineItemSnapshot.toOfflineItemSnapshotDto(): OfflineItemSnapshotDto =
    OfflineItemSnapshotDto(
        packageId = packageId,
        serverId = serverId,
        itemId = itemId,
        itemKind = itemKind,
        name = name,
        originalTitle = originalTitle,
        overview = overview,
        runtimeTicks = runtimeTicks,
        playbackPositionTicks = playbackPositionTicks,
        played = played,
        favorite = favorite,
        seriesId = seriesId,
        seriesName = seriesName,
        seasonId = seasonId,
        seasonName = seasonName,
        indexNumber = indexNumber,
        indexNumberEnd = indexNumberEnd,
        parentIndexNumber = parentIndexNumber,
        communityRating = communityRating,
        productionYear = productionYear,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis,
    )

fun OfflineItemSnapshotDto.toOfflineItemSnapshot(): OfflineItemSnapshot =
    OfflineItemSnapshot(
        packageId = packageId,
        serverId = serverId,
        itemId = itemId,
        itemKind = itemKind,
        name = name,
        originalTitle = originalTitle,
        overview = overview,
        runtimeTicks = runtimeTicks,
        playbackPositionTicks = playbackPositionTicks,
        played = played,
        favorite = favorite,
        seriesId = seriesId,
        seriesName = seriesName,
        seasonId = seasonId,
        seasonName = seasonName,
        indexNumber = indexNumber,
        indexNumberEnd = indexNumberEnd,
        parentIndexNumber = parentIndexNumber,
        communityRating = communityRating,
        productionYear = productionYear,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis,
    )
