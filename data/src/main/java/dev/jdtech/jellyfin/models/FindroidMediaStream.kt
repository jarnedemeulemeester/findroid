package dev.jdtech.jellyfin.models

import dev.jdtech.jellyfin.repository.JellyfinRepository
import org.jellyfin.sdk.model.api.MediaStream
import org.jellyfin.sdk.model.api.MediaStreamType

data class FindroidMediaStream(
    val title: String,
    val displayTitle: String?,
    val language: String,
    val type: MediaStreamType,
    val codec: String,
    val isExternal: Boolean,
    val path: String?,
    val channelLayout: String?,
    val videoRangeType: String?,
    val height: Int?,
    val width: Int?,
    val videoDoViTitle: String?,
)

fun MediaStream.toFindroidMediaStream(jellyfinRepository: JellyfinRepository): FindroidMediaStream {
    return FindroidMediaStream(
        title = title.orEmpty(),
        displayTitle = displayTitle,
        language = language.orEmpty(),
        type = type,
        codec = codec.orEmpty(),
        isExternal = isExternal,
        path = jellyfinRepository.getBaseUrl() + deliveryUrl,
        channelLayout = channelLayout,
        videoRangeType = videoRangeType,
        height = height,
        width = width,
        videoDoViTitle = videoDoViTitle,
    )
}

fun FindroidMediaStreamDto.toFindroidMediaStream(): FindroidMediaStream {
    return FindroidMediaStream(
        title = title,
        displayTitle = displayTitle,
        language = language,
        type = type,
        codec = codec,
        isExternal = isExternal,
        path = path,
        channelLayout = channelLayout,
        videoRangeType = videoRangeType,
        height = height,
        width = width,
        videoDoViTitle = videoDoViTitle,
    )
}
