package dev.jdtech.jellyfin.offline.download

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflinePackageManifestTest {
    @Test
    fun packageWithoutReadyVideoIsNotReady() {
        val manifest =
            manifest(
                assets =
                    listOf(
                        video(status = OfflineAssetStatus.DOWNLOADING),
                        poster(status = OfflineAssetStatus.READY),
                    )
            )

        assertEquals(OfflinePackageReadiness.NOT_READY, manifest.readiness)
    }

    @Test
    fun readyVideoWithMissingPosterIsOnlyPlayable() {
        val manifest =
            manifest(
                assets =
                    listOf(
                        video(status = OfflineAssetStatus.READY),
                        poster(status = OfflineAssetStatus.RETRY_WAIT),
                    )
            )

        assertEquals(OfflinePackageReadiness.PLAYABLE_READY, manifest.readiness)
    }

    @Test
    fun readyRequiredAssetsWithFailedOptionalArtworkIsPackageReady() {
        val manifest =
            manifest(
                assets =
                    listOf(
                        video(status = OfflineAssetStatus.READY),
                        poster(status = OfflineAssetStatus.READY),
                        backdrop(status = OfflineAssetStatus.FAILED_OPTIONAL),
                    )
            )

        assertEquals(OfflinePackageReadiness.PACKAGE_READY, manifest.readiness)
        assertEquals(listOf(OfflineAssetKind.BACKDROP), manifest.failedAssets().map { it.kind })
    }

    @Test
    fun readyRequiredAssetsWithSkippedOptionalArtworkIsFullyEnriched() {
        val manifest =
            manifest(
                assets =
                    listOf(
                        video(status = OfflineAssetStatus.READY),
                        poster(status = OfflineAssetStatus.READY),
                        backdrop(status = OfflineAssetStatus.SKIPPED_NOT_AVAILABLE),
                    )
            )

        assertEquals(OfflinePackageReadiness.FULLY_ENRICHED, manifest.readiness)
    }

    @Test
    fun mandatorySubtitleBlocksPlaybackUntilReady() {
        val manifest =
            manifest(
                assets =
                    listOf(
                        video(status = OfflineAssetStatus.READY),
                        poster(status = OfflineAssetStatus.READY),
                        subtitle(
                            requiredness = OfflineAssetRequiredness.PLAYBACK_REQUIRED,
                            status = OfflineAssetStatus.RETRY_WAIT,
                        ),
                    )
            )

        assertEquals(OfflinePackageReadiness.NOT_READY, manifest.readiness)
    }

    @Test
    fun optionalSubtitleFailureDoesNotBlockPackageReadiness() {
        val manifest =
            manifest(
                assets =
                    listOf(
                        video(status = OfflineAssetStatus.READY),
                        poster(status = OfflineAssetStatus.READY),
                        subtitle(
                            requiredness = OfflineAssetRequiredness.OPTIONAL,
                            status = OfflineAssetStatus.FAILED_OPTIONAL,
                        ),
                    )
            )

        assertEquals(OfflinePackageReadiness.PACKAGE_READY, manifest.readiness)
    }

    @Test
    fun initialAssetKindsDoNotIncludePeopleImages() {
        val names = OfflineAssetKind.entries.map { it.name }

        assertFalse(
            names.any { it.contains("PEOPLE") || it.contains("PERSON") || it.contains("CAST") }
        )
    }

    @Test
    fun defaultProfileIs480pAndHighProfileIsManual720p() {
        assertEquals(480, OfflineProfile.Default480p.maxHeight)
        assertEquals(720, OfflineProfile.High720p.maxHeight)
        assertTrue(OfflineProfile.Original.preserveOriginal)
    }

    private fun manifest(assets: List<OfflineAsset>): OfflinePackageManifest =
        OfflinePackageManifest(
            packageId = "pkg-1",
            itemId = "item-1",
            mediaSourceId = "source-1",
            profile = OfflineProfile.Default480p,
            projectedPath =
                ProjectedPath(
                    directorySegments = listOf("Conferences", "AvitoTech", "26"),
                    displayName = "human_name.mp4",
                ),
            assets = assets,
        )

    private fun video(status: OfflineAssetStatus): OfflineAsset =
        asset(
            kind = OfflineAssetKind.VIDEO,
            requiredness = OfflineAssetRequiredness.PLAYBACK_REQUIRED,
            storageScope = OfflineStorageScope.PUBLIC_MEDIA,
            status = status,
        )

    private fun subtitle(
        requiredness: OfflineAssetRequiredness,
        status: OfflineAssetStatus,
    ): OfflineAsset =
        asset(
            kind = OfflineAssetKind.SUBTITLE,
            requiredness = requiredness,
            storageScope = OfflineStorageScope.PUBLIC_MEDIA,
            status = status,
        )

    private fun poster(status: OfflineAssetStatus): OfflineAsset =
        asset(
            kind = OfflineAssetKind.POSTER_PRIMARY,
            requiredness = OfflineAssetRequiredness.PACKAGE_REQUIRED,
            storageScope = OfflineStorageScope.APP_PRIVATE,
            status = status,
        )

    private fun backdrop(status: OfflineAssetStatus): OfflineAsset =
        asset(
            kind = OfflineAssetKind.BACKDROP,
            requiredness = OfflineAssetRequiredness.OPTIONAL,
            storageScope = OfflineStorageScope.APP_PRIVATE,
            status = status,
        )

    private fun asset(
        kind: OfflineAssetKind,
        requiredness: OfflineAssetRequiredness,
        storageScope: OfflineStorageScope,
        status: OfflineAssetStatus,
    ): OfflineAsset =
        OfflineAsset(
            assetId = kind.name,
            packageId = "pkg-1",
            kind = kind,
            ownerItemId = "item-1",
            sourceId = "source-1",
            profileId = OfflineProfile.Default480p.id,
            storageScope = storageScope,
            requiredness = requiredness,
            status = status,
        )
}
