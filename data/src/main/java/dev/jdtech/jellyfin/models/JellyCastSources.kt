package dev.jdtech.jellyfin.models

interface JellyCastSources {
    val sources: List<JellyCastSource>
    val runtimeTicks: Long
    val trickplayInfo: Map<String, JellyCastTrickplayInfo>?
}
