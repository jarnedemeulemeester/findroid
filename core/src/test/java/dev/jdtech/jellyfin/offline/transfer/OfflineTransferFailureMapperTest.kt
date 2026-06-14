package dev.jdtech.jellyfin.offline.transfer

import dev.jdtech.jellyfin.offline.download.OfflineDownloadFailure
import dev.jdtech.jellyfin.offline.download.OfflineDownloadFailureDisposition
import dev.jdtech.jellyfin.offline.download.OfflineDownloadFailureKind
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import org.junit.Assert.assertEquals
import org.junit.Test

class OfflineTransferFailureMapperTest {
    @Test
    fun mapsHttpAuthAndPermissionErrors() {
        assertEquals(OfflineDownloadFailureKind.AuthExpired, OfflineTransferFailureMapper.fromHttpStatus(401))
        assertEquals(OfflineDownloadFailureKind.Forbidden, OfflineTransferFailureMapper.fromHttpStatus(403))
        assertEquals(
            OfflineDownloadFailureDisposition.UserAction,
            OfflineDownloadFailure(OfflineTransferFailureMapper.fromHttpStatus(401)).disposition,
        )
        assertEquals(
            OfflineDownloadFailureDisposition.UserAction,
            OfflineDownloadFailure(OfflineTransferFailureMapper.fromHttpStatus(403)).disposition,
        )
    }

    @Test
    fun mapsHttpMissingSourceAsTerminalError() {
        assertEquals(
            OfflineDownloadFailureKind.SourceMissingOrChanged,
            OfflineTransferFailureMapper.fromHttpStatus(404),
        )
        assertEquals(
            OfflineDownloadFailureDisposition.Terminal,
            OfflineDownloadFailure(OfflineTransferFailureMapper.fromHttpStatus(404)).disposition,
        )
    }

    @Test
    fun mapsHttpRetryableErrors() {
        assertEquals(OfflineDownloadFailureKind.ResumeRejected, OfflineTransferFailureMapper.fromHttpStatus(416))
        assertEquals(OfflineDownloadFailureKind.RateLimited, OfflineTransferFailureMapper.fromHttpStatus(429))
        assertEquals(OfflineDownloadFailureKind.Server5xx, OfflineTransferFailureMapper.fromHttpStatus(500))
        assertEquals(OfflineDownloadFailureKind.Server5xx, OfflineTransferFailureMapper.fromHttpStatus(502))
        assertEquals(
            OfflineDownloadFailureDisposition.Retryable,
            OfflineDownloadFailure(OfflineTransferFailureMapper.fromHttpStatus(502)).disposition,
        )
    }

    @Test
    fun mapsIoErrors() {
        assertEquals(
            OfflineDownloadFailureKind.NetworkUnavailable,
            OfflineTransferFailureMapper.fromIOException(UnknownHostException()),
        )
        assertEquals(
            OfflineDownloadFailureKind.NetworkUnavailable,
            OfflineTransferFailureMapper.fromIOException(NoRouteToHostException()),
        )
        assertEquals(
            OfflineDownloadFailureKind.ServerUnavailable,
            OfflineTransferFailureMapper.fromIOException(ConnectException()),
        )
        assertEquals(
            OfflineDownloadFailureKind.ServerUnavailable,
            OfflineTransferFailureMapper.fromIOException(SocketTimeoutException()),
        )
        assertEquals(
            OfflineDownloadFailureKind.StreamInterrupted,
            OfflineTransferFailureMapper.fromIOException(SSLException("tls")),
        )
    }
}
