package dev.jdtech.jellyfin.models

enum class ContentType(val type: String) {
    MOVIE("Movie"),
    TVSHOW("Series"),
    UNKNOWN("")
}