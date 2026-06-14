package dev.jdtech.jellyfin.models

import dev.jdtech.jellyfin.offline.download.OfflineAsset
import dev.jdtech.jellyfin.offline.download.OfflineAssetKind
import dev.jdtech.jellyfin.offline.download.OfflineAssetRequiredness
import dev.jdtech.jellyfin.offline.download.OfflineAssetStatus
import dev.jdtech.jellyfin.offline.download.OfflineDownloadFailure
import dev.jdtech.jellyfin.offline.download.OfflineDownloadFailureKind
import dev.jdtech.jellyfin.offline.download.OfflinePackageManifest
import dev.jdtech.jellyfin.offline.download.OfflinePackageReadiness
import dev.jdtech.jellyfin.offline.download.OfflineProfile
import dev.jdtech.jellyfin.offline.download.OfflineStorageScope
import dev.jdtech.jellyfin.offline.download.ProjectedPath
import org.junit.Assert.assertEquals
import org.junit.Test

class OfflinePackageDtoTest {
    @Test
    fun packageRoundTripPreservesProfilePathAndReadiness() {
        val manifest =
            OfflinePackageManifest(
                packageId = "pkg-1",
                itemId = "item-1",
                mediaSourceId = "source-1",
                profile = OfflineProfile.Default480p,
                projectedPath =
                    ProjectedPath(
                        directorySegments = listOf("Конференции", "AvitoTech", "26"),
                        displayName = "human_name.mp4",
                    ),
                assets =
                    listOf(
                        asset(
                            kind = OfflineAssetKind.VIDEO,
                            requiredness = OfflineAssetRequiredness.PLAYBACK_REQUIRED,
                            status = OfflineAssetStatus.READY,
                        ),
                        asset(
                            kind = OfflineAssetKind.POSTER_PRIMARY,
                            requiredness = OfflineAssetRequiredness.PACKAGE_REQUIRED,
                            status = OfflineAssetStatus.READY,
                        ),
                    ),
            )
        val packageDto =
            manifest.toOfflinePackageDto(
                serverId = "server-1",
                createdAtMillis = 10,
                updatedAtMillis = 20,
            )
        val assetDtos = manifest.assets.map { it.toOfflineAssetDto(updatedAtMillis = 20) }

        val roundTrip = packageDto.toOfflinePackageManifest(assetDtos)

        assertEquals("server-1", packageDto.serverId)
        assertEquals(OfflinePackageReadiness.FULLY_ENRICHED, packageDto.readiness)
        assertEquals(manifest.profile, roundTrip.profile)
        assertEquals(manifest.projectedPath, roundTrip.projectedPath)
        assertEquals(manifest.readiness, roundTrip.readiness)
    }

    @Test
    fun assetRoundTripPreservesFailureAndRetryState() {
        val failure = OfflineDownloadFailure(OfflineDownloadFailureKind.StreamInterrupted, "cut")
        val dto =
            asset(
                    kind = OfflineAssetKind.BACKDROP,
                    requiredness = OfflineAssetRequiredness.OPTIONAL,
                    status = OfflineAssetStatus.FAILED_OPTIONAL,
                    failure = failure,
                    retryCount = 3,
                )
                .toOfflineAssetDto(updatedAtMillis = 99)

        val roundTrip = dto.toOfflineAsset()

        assertEquals(OfflineAssetKind.BACKDROP, roundTrip.kind)
        assertEquals(OfflineAssetStatus.FAILED_OPTIONAL, roundTrip.status)
        assertEquals(failure, roundTrip.failure)
        assertEquals(3, roundTrip.retryCount)
        assertEquals(99, dto.updatedAtMillis)
    }

    private fun asset(
        kind: OfflineAssetKind,
        requiredness: OfflineAssetRequiredness,
        status: OfflineAssetStatus,
        failure: OfflineDownloadFailure? = null,
        retryCount: Int = 0,
    ): OfflineAsset =
        OfflineAsset(
            assetId = kind.name,
            packageId = "pkg-1",
            kind = kind,
            ownerItemId = "item-1",
            sourceId = "source-1",
            profileId = OfflineProfile.Default480p.id,
            storageScope = OfflineStorageScope.APP_PRIVATE,
            requiredness = requiredness,
            status = status,
            failure = failure,
            retryCount = retryCount,
        )
}
