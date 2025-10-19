package dev.jdtech.jellyfin.models

import org.jellyfin.sdk.model.api.TrickplayInfo

data class JellyCastTrickplayInfo(
    val width: Int,
    val height: Int,
    val tileWidth: Int,
    val tileHeight: Int,
    val thumbnailCount: Int,
    val interval: Int,
    val bandwidth: Int,
)

fun TrickplayInfo.toJellyCastTrickplayInfo(): JellyCastTrickplayInfo {
    return JellyCastTrickplayInfo(
        width = width,
        height = height,
        tileWidth = tileWidth,
        tileHeight = tileHeight,
        thumbnailCount = thumbnailCount,
        interval = interval,
        bandwidth = bandwidth,
    )
}

fun JellyCastTrickplayInfoDto.toJellyCastTrickplayInfo(): JellyCastTrickplayInfo {
    return JellyCastTrickplayInfo(
        width = width,
        height = height,
        tileWidth = tileWidth,
        tileHeight = tileHeight,
        thumbnailCount = thumbnailCount,
        interval = interval,
        bandwidth = bandwidth,
    )
}
