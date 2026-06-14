package dev.jdtech.jellyfin.car

import dev.jdtech.jellyfin.models.FindroidSource
import dev.jdtech.jellyfin.models.FindroidSourceType
import org.junit.Assert.assertEquals
import org.junit.Test

class FindroidCarPlaybackTimelineTest {
    @Test
    fun onlineItemUsesOnlineSourceKindWithoutRestartRequest() {
        assertEquals(FindroidCarPlaybackSourceKind.ONLINE, catalogItem(videoPath = null).playbackSourceKind())
    }

    @Test
    fun localItemUsesLocalSourceKind() {
        assertEquals(
            FindroidCarPlaybackSourceKind.LOCAL,
            catalogItem(videoPath = "/sdcard/Movies/Findroid/E01.mp4").playbackSourceKind(),
        )
    }

    @Test
    fun absoluteTimelineUsesPlayerPositionWhenOffsetIsZero() {
        assertEquals(
            42_500L,
            FindroidCarPlaybackTimeline.absolutePositionMs(
                playerPositionMs = 42_500L,
                streamStartPositionMs = 0L,
                durationMs = 120_000L,
            ),
        )
    }

    @Test
    fun absoluteTimelineClampsToDuration() {
        assertEquals(
            120_000L,
            FindroidCarPlaybackTimeline.absolutePositionMs(
                playerPositionMs = 150_000L,
                streamStartPositionMs = 0L,
                durationMs = 120_000L,
            ),
        )
    }

    @Test
    fun startPositionUsesNativePlayerPositionAboveResumeThreshold() {
        assertEquals(42_000L, findroidCarStartPositionMs(420_000_000L))
    }

    @Test
    fun startPositionStartsAtBeginningForTinyResumePosition() {
        assertEquals(0L, findroidCarStartPositionMs(50_000_000L))
    }

    @Test
    fun standardPlaybackSourcePrefersLocalPathLikePhonePlayer() {
        val source =
            listOf(
                    source("remote", FindroidSourceType.REMOTE, "https://example.test/stream.mp4"),
                    source("local", FindroidSourceType.LOCAL, "/sdcard/Movies/Findroid/E01.mp4"),
                )
                .findroidStandardPlaybackSource()

        assertEquals("local", source?.id)
    }

    @Test
    fun standardPlaybackSourceFallsBackToFirstPlayablePath() {
        val source =
            listOf(
                    source("empty-local", FindroidSourceType.LOCAL, ""),
                    source("remote", FindroidSourceType.REMOTE, "https://example.test/stream.mp4"),
                )
                .findroidStandardPlaybackSource()

        assertEquals("remote", source?.id)
    }

    private fun catalogItem(videoPath: String?): FindroidCarCatalogItem =
        FindroidCarCatalogItem(
            packageId = "online:$ITEM_ID",
            itemId = ITEM_ID,
            itemKind = FindroidCarCatalogItemKind.EPISODE,
            playerItemKind = "Episode",
            seriesId = SERIES_ID,
            seriesName = "Series",
            seasonId = SEASON_ID,
            seasonName = "Season 1",
            indexNumber = 1,
            parentIndexNumber = 1,
            title = "E01",
            subtitle = "Series / Season 1",
            runtimeText = "57 min",
            runtimeTicks = 34_200_000_000L,
            artworkPaths = emptyList(),
            videoPath = videoPath,
            streamUrl = "https://example.test/Videos/$ITEM_ID/stream",
            played = false,
            favorite = false,
            playbackPositionTicks = 0L,
            unplayedItemCount = null,
        )

    private fun source(id: String, type: FindroidSourceType, path: String): FindroidSource =
        FindroidSource(
            id = id,
            name = id,
            type = type,
            path = path,
            size = 1L,
            mediaStreams = emptyList(),
        )

    private companion object {
        const val ITEM_ID = "11111111-1111-1111-1111-111111111111"
        const val SERIES_ID = "22222222-2222-2222-2222-222222222222"
        const val SEASON_ID = "33333333-3333-3333-3333-333333333333"
    }
}
