package dev.jdtech.jellyfin.core.presentation.downloader

import dev.jdtech.jellyfin.models.UiText
import dev.jdtech.jellyfin.offline.download.OfflineDownloadFailure
import dev.jdtech.jellyfin.offline.download.OfflineDownloadFailureKind
import org.junit.Assert.assertEquals
import org.junit.Test

class DownloaderErrorTextTest {
    @Test
    fun authExpiredMessageTellsUserToSignInAgain() {
        val message =
            OfflineDownloadFailure(OfflineDownloadFailureKind.AuthExpired)
                .toDownloaderErrorText() as UiText.DynamicString

        assertEquals("Jellyfin login expired. Sign in again.", message.value)
    }

    @Test
    fun server5xxMessageExplainsAutomaticRetry() {
        val message =
            OfflineDownloadFailure(OfflineDownloadFailureKind.Server5xx)
                .toDownloaderErrorText() as UiText.DynamicString

        assertEquals("Jellyfin server error. The download will retry.", message.value)
    }

    @Test
    fun technicalFailureMessageDoesNotReplaceUserMessage() {
        val message =
            OfflineDownloadFailure(OfflineDownloadFailureKind.ServerUnavailable, "HTTP 502")
                .toDownloaderErrorText() as UiText.DynamicString

        assertEquals("Jellyfin server is unavailable. The download will retry.", message.value)
    }
}
