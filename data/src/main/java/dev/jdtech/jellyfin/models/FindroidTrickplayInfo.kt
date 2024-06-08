package dev.jdtech.jellyfin.models

import org.jellyfin.sdk.model.api.TrickplayInfo

data class FindroidTrickplayInfo(
    val width: Int,
    val height: Int,
    val tileWidth: Int,
    val tileHeight: Int,
    val thumbnailCount: Int,
    val interval: Int,
    val bandwidth: Int,
)

fun TrickplayInfo.toFindroidTrickplayInfo(): FindroidTrickplayInfo {
    return FindroidTrickplayInfo(
        width = width,
        height = height,
        tileWidth = tileWidth,
        tileHeight = tileHeight,
        thumbnailCount = thumbnailCount,
        interval = interval,
        bandwidth = bandwidth,
    )
}

fun FindroidTrickplayInfoDto.toFindroidTrickplayInfo(): FindroidTrickplayInfo {
    return FindroidTrickplayInfo(
        width = width,
        height = height,
        tileWidth = tileWidth,
        tileHeight = tileHeight,
        thumbnailCount = thumbnailCount,
        interval = interval,
        bandwidth = bandwidth,
    )
}
