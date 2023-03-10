package dev.jdtech.jellyfin.utils

import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.FindroidSource
import dev.jdtech.jellyfin.models.TrickPlayManifest

interface Downloader {
    suspend fun downloadItem(
        item: FindroidItem,
        source: FindroidSource,
        trickPlayManifest: TrickPlayManifest?,
        trickPlayData: ByteArray?,
        serverId: String,
        baseUrl: String
    ): Long

    suspend fun deleteItem(item: FindroidItem, source: FindroidSource)

    suspend fun getProgress(downloadId: Long?): Pair<Int, Int>
}
