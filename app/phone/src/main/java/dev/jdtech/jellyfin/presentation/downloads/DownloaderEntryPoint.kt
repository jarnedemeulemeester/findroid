package dev.jdtech.jellyfin.presentation.downloads

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.jdtech.jellyfin.utils.Downloader

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DownloaderEntryPoint {
    fun downloader(): Downloader
}
