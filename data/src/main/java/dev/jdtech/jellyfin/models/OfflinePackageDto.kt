package dev.jdtech.jellyfin.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.jdtech.jellyfin.offline.download.OfflinePackageManifest
import dev.jdtech.jellyfin.offline.download.OfflinePackageReadiness
import dev.jdtech.jellyfin.offline.download.OfflineProfile
import dev.jdtech.jellyfin.offline.download.OfflineProfileKind
import dev.jdtech.jellyfin.offline.download.ProjectedPath

@Entity(
    tableName = "offlinePackages",
    indices = [Index("serverId"), Index("itemId"), Index("mediaSourceId"), Index("profileId")],
)
data class OfflinePackageDto(
    @PrimaryKey val packageId: String,
    val serverId: String,
    val itemId: String,
    val mediaSourceId: String,
    val profileId: String,
    val profileKind: OfflineProfileKind,
    val container: String,
    val videoCodec: String?,
    val audioCodec: String?,
    val maxHeight: Int?,
    val videoBitrateBitsPerSecond: Int?,
    val audioBitrateBitsPerSecond: Int?,
    val preserveOriginal: Boolean,
    val relativeDirectory: String,
    val displayName: String,
    val readiness: OfflinePackageReadiness,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

fun OfflinePackageManifest.toOfflinePackageDto(
    serverId: String,
    createdAtMillis: Long,
    updatedAtMillis: Long,
): OfflinePackageDto =
    OfflinePackageDto(
        packageId = packageId,
        serverId = serverId,
        itemId = itemId,
        mediaSourceId = mediaSourceId,
        profileId = profile.id,
        profileKind = profile.kind,
        container = profile.container,
        videoCodec = profile.videoCodec,
        audioCodec = profile.audioCodec,
        maxHeight = profile.maxHeight,
        videoBitrateBitsPerSecond = profile.videoBitrateBitsPerSecond,
        audioBitrateBitsPerSecond = profile.audioBitrateBitsPerSecond,
        preserveOriginal = profile.preserveOriginal,
        relativeDirectory = projectedPath.relativeDirectory,
        displayName = projectedPath.displayName,
        readiness = readiness,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis,
    )

fun OfflinePackageDto.toOfflinePackageManifest(assets: List<OfflineAssetDto>): OfflinePackageManifest =
    OfflinePackageManifest(
        packageId = packageId,
        itemId = itemId,
        mediaSourceId = mediaSourceId,
        profile =
            OfflineProfile(
                id = profileId,
                kind = profileKind,
                container = container,
                videoCodec = videoCodec,
                audioCodec = audioCodec,
                maxHeight = maxHeight,
                videoBitrateBitsPerSecond = videoBitrateBitsPerSecond,
                audioBitrateBitsPerSecond = audioBitrateBitsPerSecond,
                preserveOriginal = preserveOriginal,
            ),
        projectedPath =
            ProjectedPath(
                directorySegments = relativeDirectory.split('/').filter { it.isNotBlank() },
                displayName = displayName,
            ),
        assets = assets.map { it.toOfflineAsset() },
    )
