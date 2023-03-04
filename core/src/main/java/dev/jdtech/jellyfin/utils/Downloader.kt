package dev.jdtech.jellyfin.utils

import dev.jdtech.jellyfin.models.JellyfinItem
import dev.jdtech.jellyfin.models.JellyfinSource

interface Downloader {
    suspend fun downloadItem(item: JellyfinItem, source: JellyfinSource): Long
}
