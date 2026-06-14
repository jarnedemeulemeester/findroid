package dev.jdtech.jellyfin.offline.download

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflinePackageManifestFactoryTest {
    private val factory = OfflinePackageManifestFactory()

    @Test
    fun defaultProfileCreatesPublicMp4VideoPackage() {
        val result =
            factory.create(
                OfflinePackageManifestInput(
                    itemId = ITEM_ID,
                    mediaSourceId = SOURCE_ID,
                    logicalDirectorySegments = listOf("Конференции", "AvitoTech", "26"),
                    baseFileName = "human_name.mkv",
                    sourceExtension = "mkv",
                    profile = OfflineProfile.Default480p,
                )
            )

        assertTrue(result is OfflinePackageManifestFactoryResult.Success)
        val manifest = (result as OfflinePackageManifestFactoryResult.Success).manifest
        assertEquals(
            "Конференции/AvitoTech/26/human_name.mp4",
            manifest.projectedPath.relativeFilePath,
        )
        assertEquals(OfflineProfile.Default480p, manifest.profile)
        assertEquals(OfflineAssetKind.VIDEO, manifest.assets.single().kind)
        assertEquals(OfflineStorageScope.PUBLIC_MEDIA, manifest.assets.single().storageScope)
        assertEquals(
            OfflineAssetRequiredness.PLAYBACK_REQUIRED,
            manifest.assets.single().requiredness,
        )
        assertEquals("video/mp4", manifest.assets.single().mimeType)
    }

    @Test
    fun seriesEpisodeKeepsJellyfinLogicalTreeAndSeasonIndex() {
        val result =
            factory.create(
                OfflinePackageManifestInput(
                    itemId = ITEM_ID,
                    mediaSourceId = SOURCE_ID,
                    logicalDirectorySegments = listOf("Сериалы", "История любви", "1"),
                    baseFileName = "E01 - Первая серия.mkv",
                    sourceExtension = "mkv",
                    profile = OfflineProfile.Default480p,
                )
            )

        assertTrue(result is OfflinePackageManifestFactoryResult.Success)
        val manifest = (result as OfflinePackageManifestFactoryResult.Success).manifest
        assertEquals(
            "Сериалы/История любви/1/E01 - Первая серия.mp4",
            manifest.projectedPath.relativeFilePath,
        )
    }

    @Test
    fun originalProfileKeepsSourceExtension() {
        val result =
            factory.create(
                OfflinePackageManifestInput(
                    itemId = ITEM_ID,
                    mediaSourceId = SOURCE_ID,
                    logicalDirectorySegments = listOf("Movies"),
                    baseFileName = "Film",
                    sourceExtension = "mkv",
                    profile = OfflineProfile.Original,
                )
            )

        assertTrue(result is OfflinePackageManifestFactoryResult.Success)
        val manifest = (result as OfflinePackageManifestFactoryResult.Success).manifest
        assertEquals("Movies/Film.mkv", manifest.projectedPath.relativeFilePath)
        assertEquals(OfflineProfile.Original, manifest.profile)
    }

    @Test
    fun failsWhenLogicalPathIsUnavailable() {
        val result =
            factory.create(
                OfflinePackageManifestInput(
                    itemId = ITEM_ID,
                    mediaSourceId = SOURCE_ID,
                    logicalDirectorySegments = emptyList(),
                    baseFileName = "Film.mkv",
                    sourceExtension = "mkv",
                    profile = OfflineProfile.Default480p,
                )
            )

        assertEquals(
            ProjectedPathFailureKind.MissingLogicalDirectory,
            (result as OfflinePackageManifestFactoryResult.Failure).failure.kind,
        )
    }

    private companion object {
        const val ITEM_ID = "00000000-0000-0000-0000-000000000001"
        const val SOURCE_ID = "source"
    }
}
