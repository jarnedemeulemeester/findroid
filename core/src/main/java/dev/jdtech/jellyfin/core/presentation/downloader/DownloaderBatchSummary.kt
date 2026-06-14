package dev.jdtech.jellyfin.core.presentation.downloader

import dev.jdtech.jellyfin.models.UiText

internal data class DownloaderBatchSummary(
    val readyEpisodes: Int,
    val totalEpisodes: Int,
    val failedEpisodes: Int = 0,
) {
    val isComplete: Boolean
        get() = totalEpisodes > 0 && readyEpisodes == totalEpisodes

    fun toDetailsText(): UiText? {
        if (totalEpisodes <= 0) return null
        val safeReady = readyEpisodes.coerceIn(0, totalEpisodes)
        val safeFailed = failedEpisodes.coerceIn(0, totalEpisodes - safeReady)
        val text =
            if (safeFailed > 0) {
                "Episodes: $safeReady/$totalEpisodes downloaded, $safeFailed failed."
            } else {
                "Episodes: $safeReady/$totalEpisodes downloaded."
            }
        return UiText.DynamicString(text)
    }
}
