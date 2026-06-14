package dev.jdtech.jellyfin.repository

import dev.jdtech.jellyfin.models.FindroidSource

internal fun mergeOfflineFirstMediaSources(
    readyOfflineSource: FindroidSource?,
    remoteSources: List<FindroidSource> = emptyList(),
    legacyLocalSources: List<FindroidSource> = emptyList(),
): List<FindroidSource> =
    buildList {
        readyOfflineSource?.let(::add)
        addAll(remoteSources)
        addAll(legacyLocalSources)
    }
