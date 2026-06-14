package dev.jdtech.jellyfin.core.presentation.downloader

import dev.jdtech.jellyfin.models.UiText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloaderBatchSummaryTest {
    @Test
    fun emptyBatchHasNoDetailsText() {
        assertNull(
            DownloaderBatchSummary(
                    readyEpisodes = 0,
                    totalEpisodes = 0,
                )
                .toDetailsText()
        )
    }

    @Test
    fun inProgressBatchShowsDownloadedEpisodeCount() {
        val text =
            DownloaderBatchSummary(
                    readyEpisodes = 3,
                    totalEpisodes = 9,
                )
                .toDetailsText() as UiText.DynamicString

        assertEquals("Episodes: 3/9 downloaded.", text.value)
    }

    @Test
    fun failedBatchShowsFailedEpisodeCount() {
        val text =
            DownloaderBatchSummary(
                    readyEpisodes = 3,
                    totalEpisodes = 9,
                    failedEpisodes = 2,
                )
                .toDetailsText() as UiText.DynamicString

        assertEquals("Episodes: 3/9 downloaded, 2 failed.", text.value)
    }

    @Test
    fun completeBatchIsCompleteOnlyWhenEveryEpisodeIsReady() {
        assertTrue(
            DownloaderBatchSummary(
                    readyEpisodes = 9,
                    totalEpisodes = 9,
                )
                .isComplete
        )
    }
}
