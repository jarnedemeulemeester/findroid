package dev.jdtech.jellyfin.models

interface FindroidSources {
    val sources: List<FindroidSource>
    val playedPercentage: Float?
    val runtimeTicks: Long
}
