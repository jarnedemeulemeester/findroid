package dev.jdtech.jellyfin.core.presentation.downloader

import android.app.DownloadManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloaderStateTest {
    @Test
    fun pausedRetryWaitStateRemainsVisibleAsActiveDownload() {
        assertTrue(DownloaderState(status = DownloadManager.STATUS_PAUSED).isDownloading)
    }

    @Test
    fun successfulStateIsNotActiveDownload() {
        assertFalse(DownloaderState(status = DownloadManager.STATUS_SUCCESSFUL).isDownloading)
    }

    @Test
    fun zeroProgressIsIndeterminate() {
        assertFalse(
            DownloaderState(status = DownloadManager.STATUS_RUNNING, progress = 0f)
                .hasDeterminateProgress
        )
    }

    @Test
    fun positiveProgressIsDeterminate() {
        assertTrue(
            DownloaderState(status = DownloadManager.STATUS_RUNNING, progress = 0.25f)
                .hasDeterminateProgress
        )
    }

    @Test
    fun successfulStateIsDeterminateEvenWithZeroProgress() {
        assertTrue(
            DownloaderState(status = DownloadManager.STATUS_SUCCESSFUL, progress = 0f)
                .hasDeterminateProgress
        )
    }
}
