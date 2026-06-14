package dev.jdtech.jellyfin.offline.storage

import dev.jdtech.jellyfin.offline.download.OfflineDownloadFailureKind
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Test

class OfflineStorageFailureMapperTest {
    @Test
    fun mapsNoSpaceErrorsToInsufficientSpace() {
        assertEquals(
            OfflineDownloadFailureKind.InsufficientSpace,
            OfflineStorageFailureMapper.fromIOException(IOException("ENOSPC")),
        )
        assertEquals(
            OfflineDownloadFailureKind.InsufficientSpace,
            OfflineStorageFailureMapper.fromIOException(IOException("No space left on device")),
        )
        assertEquals(
            OfflineDownloadFailureKind.InsufficientSpace,
            OfflineStorageFailureMapper.fromIOException(
                IOException("sync failed", IOException("write failed: ENOSPC")),
            ),
        )
        assertEquals(
            OfflineDownloadFailureKind.InsufficientSpace,
            OfflineStorageFailureMapper.fromIOException(
                IOException("sync failed"),
                hasNoUsableSpace = true,
            ),
        )
    }

    @Test
    fun mapsPermissionErrorsToPermissionRequired() {
        assertEquals(
            OfflineDownloadFailureKind.PermissionRequired,
            OfflineStorageFailureMapper.fromIOException(IOException("Permission denied")),
        )
    }

    @Test
    fun mapsUnknownStorageErrorsToStorageRootUnavailable() {
        assertEquals(
            OfflineDownloadFailureKind.StorageRootUnavailable,
            OfflineStorageFailureMapper.fromIOException(IOException("I/O error")),
        )
    }
}
