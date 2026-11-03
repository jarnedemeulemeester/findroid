package dev.jdtech.jellyfin.settings.domain.models

enum class PlayerBackend(val value: String) {
    EXOPLAYER("exoplayer"),
    MPV("mpv");

    companion object {
        fun fromValue(value: String): PlayerBackend =
            entries.firstOrNull { it.value == value } ?: EXOPLAYER
    }
}
