package dev.jdtech.jellyfin.models

import dev.jdtech.jellyfin.models.ContentType.MOVIE
import dev.jdtech.jellyfin.models.ContentType.TVSHOW
import dev.jdtech.jellyfin.models.ContentType.UNKNOWN
import org.jellyfin.sdk.model.api.BaseItemDto

enum class ContentType(val type: String) {

    MOVIE("Movie"), TVSHOW("Series"), UNKNOWN("")
}

fun BaseItemDto.contentType() = when (type) {
    "Movie" -> MOVIE
    "Series" -> TVSHOW
    else -> UNKNOWN
}