package dev.jdtech.jellyfin.utils

import dev.jdtech.jellyfin.models.JellyCastItem
import dev.jdtech.jellyfin.models.JellyCastSource
import dev.jdtech.jellyfin.models.UiText

interface Downloader {
    suspend fun downloadItem(
        item: JellyCastItem,
        sourceId: String,
        storageIndex: Int = 0,
    ): Pair<Long, UiText?>

    suspend fun cancelDownload(item: JellyCastItem, source: JellyCastSource)

    suspend fun deleteItem(item: JellyCastItem, source: JellyCastSource)

    suspend fun getProgress(downloadId: Long?): Pair<Int, Int>
}
