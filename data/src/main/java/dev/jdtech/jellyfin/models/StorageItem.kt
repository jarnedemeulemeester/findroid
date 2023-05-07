package dev.jdtech.jellyfin.models

data class StorageItem(
    val item: FindroidItem,
    val size: Long? = null,
    val externalSubtitles: Boolean = false,
    val externalSubtitlesSize: Long? = null,
    val trickPlayData: Boolean = false,
    val trickPlayDataSize: Long? = null,
    val introTimestamps: Boolean = false,
    val indent: Int = 0,
)
