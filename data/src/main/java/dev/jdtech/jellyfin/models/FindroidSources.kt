package dev.jdtech.jellyfin.models

interface FindroidSources {
    val sources: List<FindroidSource>
    val runtimeTicks: Long
    val trickplayInfo: Map<String, Map<String, FindroidTrickplayInfo>>?
}
