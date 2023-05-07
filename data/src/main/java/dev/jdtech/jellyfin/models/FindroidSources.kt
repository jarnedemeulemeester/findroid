package dev.jdtech.jellyfin.models

interface FindroidSources {
    val sources: List<FindroidSource>
    val runtimeTicks: Long
}
