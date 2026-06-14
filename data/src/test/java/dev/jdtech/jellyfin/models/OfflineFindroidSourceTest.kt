package dev.jdtech.jellyfin.models

import dev.jdtech.jellyfin.offline.download.OfflineAssetKind
import dev.jdtech.jellyfin.offline.download.OfflineAssetRequiredness
import dev.jdtech.jellyfin.offline.download.OfflineAssetStatus
import dev.jdtech.jellyfin.offline.download.OfflineStorageScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OfflineFindroidSourceTest {
    @Test
    fun readyPublicVideoAssetBecomesLocalSource() {
        val source =
            offlineAsset(
                    kind = OfflineAssetKind.VIDEO,
                    storageScope = OfflineStorageScope.PUBLIC_MEDIA,
                    status = OfflineAssetStatus.READY,
                    finalPath = "/storage/emulated/0/Movies/Findroid/video.mp4",
                    bytes = 42L,
                )
                .toOfflineFindroidSource()

        requireNotNull(source)
        assertEquals(FindroidSourceType.LOCAL, source.type)
        assertEquals("/storage/emulated/0/Movies/Findroid/video.mp4", source.path)
        assertEquals(42L, source.size)
    }

    @Test
    fun nonReadyVideoAssetDoesNotBecomePlayableSource() {
        assertNull(
            offlineAsset(
                    kind = OfflineAssetKind.VIDEO,
                    storageScope = OfflineStorageScope.PUBLIC_MEDIA,
                    status = OfflineAssetStatus.DOWNLOADING,
                    finalPath = "/storage/emulated/0/Movies/Findroid/video.mp4",
                )
                .toOfflineFindroidSource()
        )
    }

    @Test
    fun privateArtworkDoesNotBecomePlayableSource() {
        assertNull(
            offlineAsset(
                    kind = OfflineAssetKind.POSTER_PRIMARY,
                    storageScope = OfflineStorageScope.APP_PRIVATE,
                    status = OfflineAssetStatus.READY,
                    finalPath = "/data/user/0/app/files/images/item/primary",
                )
                .toOfflineFindroidSource()
        )
    }

    private fun offlineAsset(
        kind: OfflineAssetKind,
        storageScope: OfflineStorageScope,
        status: OfflineAssetStatus,
        finalPath: String?,
        bytes: Long? = null,
    ): OfflineAssetDto =
        OfflineAssetDto(
            assetId = "asset",
            packageId = "package",
            kind = kind,
            ownerItemId = "item",
            sourceId = "source",
            storageScope = storageScope,
            finalPath = finalPath,
            bytes = bytes,
            requiredness = OfflineAssetRequiredness.PLAYBACK_REQUIRED,
            status = status,
            updatedAtMillis = 1L,
        )
}
