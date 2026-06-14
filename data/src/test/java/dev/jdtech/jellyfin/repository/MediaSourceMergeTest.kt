package dev.jdtech.jellyfin.repository

import dev.jdtech.jellyfin.models.FindroidSource
import dev.jdtech.jellyfin.models.FindroidSourceType
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaSourceMergeTest {
    @Test
    fun readyOfflineSourceIsFirst() {
        val merged =
            mergeOfflineFirstMediaSources(
                readyOfflineSource = source("offline", FindroidSourceType.LOCAL),
                remoteSources = listOf(source("remote", FindroidSourceType.REMOTE)),
                legacyLocalSources = listOf(source("legacy", FindroidSourceType.LOCAL)),
            )

        assertEquals(listOf("offline", "remote", "legacy"), merged.map { it.id })
    }

    @Test
    fun remoteAndLegacySourcesRemainWhenNoReadyOfflineSourceExists() {
        val merged =
            mergeOfflineFirstMediaSources(
                readyOfflineSource = null,
                remoteSources = listOf(source("remote", FindroidSourceType.REMOTE)),
                legacyLocalSources = listOf(source("legacy", FindroidSourceType.LOCAL)),
            )

        assertEquals(listOf("remote", "legacy"), merged.map { it.id })
    }

    private fun source(id: String, type: FindroidSourceType): FindroidSource =
        FindroidSource(
            id = id,
            name = id,
            type = type,
            path = "/tmp/$id.mp4",
            size = 1L,
            mediaStreams = emptyList(),
        )
}
