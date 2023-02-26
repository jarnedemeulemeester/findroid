package dev.jdtech.jellyfin.models

interface JellyfinSources {
    val sources: List<JellyfinSource>
    val playedPercentage: Float?
    val runtimeTicks: Long
}
