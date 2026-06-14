package dev.jdtech.jellyfin.car

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FindroidCarContinueWatchingResolverTest {
    @Test
    fun serverResumeDownloadedItemUsesOneRowWithLocalVideoPath() {
        val serverItem = item(itemId = ITEM_ID, playbackPositionTicks = 60)
        val offlineItem = item(itemId = ITEM_ID, playbackPositionTicks = 10, videoPath = "/local.mp4")

        val result =
            FindroidCarContinueWatchingResolver.resolve(
                serverResumeItems = listOf(serverItem),
                offlineItems = listOf(offlineItem),
                historyEntries = emptyList(),
                userDataByItemId = emptyMap(),
                maxItems = 10,
            )

        assertEquals(1, result.size)
        assertEquals("/local.mp4", result.single().videoPath)
        assertEquals(null, result.single().streamUrl)
        assertEquals(60, result.single().playbackPositionTicks)
    }

    @Test
    fun historyServerAndDownloadedOverlapUsesOneLocalRowWithCanonicalProgress() {
        val serverItem = item(itemId = ITEM_ID, playbackPositionTicks = 60)
        val offlineItem = item(itemId = ITEM_ID, playbackPositionTicks = 10, videoPath = "/downloaded.mp4")
        val historyItem = item(itemId = ITEM_ID, playbackPositionTicks = 90, videoPath = "/history.mp4")

        val result =
            FindroidCarContinueWatchingResolver.resolve(
                serverResumeItems = listOf(serverItem),
                offlineItems = listOf(offlineItem),
                historyEntries =
                    listOf(
                        FindroidCarPlaybackHistory.Entry(
                            updatedAtMillis = 1,
                            item = historyItem,
                        )
                    ),
                userDataByItemId =
                    mapOf(
                        ITEM_ID to
                            FindroidCarUserDataOverlay(
                                played = false,
                                playbackPositionTicks = 120,
                                toBeSynced = true,
                            )
                    ),
                maxItems = 10,
            )

        assertEquals(1, result.size)
        assertEquals("/downloaded.mp4", result.single().videoPath)
        assertEquals(null, result.single().streamUrl)
        assertEquals(120, result.single().playbackPositionTicks)
    }

    @Test
    fun dirtyLocalUserDataOverridesServerResumeProgress() {
        val result =
            FindroidCarContinueWatchingResolver.resolve(
                serverResumeItems = listOf(item(itemId = ITEM_ID, playbackPositionTicks = 60)),
                offlineItems = emptyList(),
                historyEntries = emptyList(),
                userDataByItemId =
                    mapOf(
                        ITEM_ID to
                            FindroidCarUserDataOverlay(
                                played = false,
                                playbackPositionTicks = 120,
                                toBeSynced = true,
                            )
                    ),
                maxItems = 10,
            )

        assertEquals(1, result.size)
        assertEquals(120, result.single().playbackPositionTicks)
    }

    @Test
    fun nonDirtyLocalUserDataDoesNotHideServerResumeProgress() {
        val result =
            FindroidCarContinueWatchingResolver.resolve(
                serverResumeItems = listOf(item(itemId = ITEM_ID, playbackPositionTicks = 60)),
                offlineItems = emptyList(),
                historyEntries = emptyList(),
                userDataByItemId =
                    mapOf(
                        ITEM_ID to
                            FindroidCarUserDataOverlay(
                                played = false,
                                playbackPositionTicks = 0,
                                toBeSynced = false,
                            )
                    ),
                maxItems = 10,
            )

        assertEquals(1, result.size)
        assertEquals(60, result.single().playbackPositionTicks)
    }

    @Test
    fun offlineItemUsesLocalUserDataProgress() {
        val result =
            FindroidCarContinueWatchingResolver.resolve(
                serverResumeItems = emptyList(),
                offlineItems = listOf(item(itemId = ITEM_ID, playbackPositionTicks = 0, videoPath = "/local.mp4")),
                historyEntries = emptyList(),
                userDataByItemId =
                    mapOf(
                        ITEM_ID to
                            FindroidCarUserDataOverlay(
                                played = false,
                                playbackPositionTicks = 90,
                                toBeSynced = false,
                            )
                    ),
                maxItems = 10,
            )

        assertEquals(1, result.size)
        assertEquals(90, result.single().playbackPositionTicks)
    }

    @Test
    fun localHistoryDoesNotResurrectWatchedItem() {
        val result =
            FindroidCarContinueWatchingResolver.resolve(
                serverResumeItems = emptyList(),
                offlineItems = emptyList(),
                historyEntries =
                    listOf(
                        FindroidCarPlaybackHistory.Entry(
                            updatedAtMillis = 1,
                            item = item(itemId = ITEM_ID, playbackPositionTicks = 60),
                        )
                    ),
                userDataByItemId =
                    mapOf(
                        ITEM_ID to
                            FindroidCarUserDataOverlay(
                                played = true,
                                playbackPositionTicks = 0,
                                toBeSynced = true,
                            )
                    ),
                maxItems = 10,
            )

        assertTrue(result.isEmpty())
    }

    @Test
    fun watchedLocalUserDataDoesNotResurrectStaleServerResume() {
        val result =
            FindroidCarContinueWatchingResolver.resolve(
                serverResumeItems = listOf(item(itemId = ITEM_ID, playbackPositionTicks = 60)),
                offlineItems = emptyList(),
                historyEntries = emptyList(),
                userDataByItemId =
                    mapOf(
                        ITEM_ID to
                            FindroidCarUserDataOverlay(
                                played = true,
                                playbackPositionTicks = 0,
                                toBeSynced = false,
                            )
                    ),
                maxItems = 10,
            )

        assertTrue(result.isEmpty())
    }

    @Test
    fun watchedLocalUserDataDoesNotResurrectDownloadedCopy() {
        val result =
            FindroidCarContinueWatchingResolver.resolve(
                serverResumeItems = listOf(item(itemId = ITEM_ID, playbackPositionTicks = 60)),
                offlineItems = listOf(item(itemId = ITEM_ID, playbackPositionTicks = 60, videoPath = "/local.mp4")),
                historyEntries = emptyList(),
                userDataByItemId =
                    mapOf(
                        ITEM_ID to
                            FindroidCarUserDataOverlay(
                                played = true,
                                playbackPositionTicks = 0,
                                toBeSynced = false,
                            )
                    ),
                maxItems = 10,
            )

        assertTrue(result.isEmpty())
    }

    private fun item(
        itemId: String,
        playbackPositionTicks: Long,
        played: Boolean = false,
        videoPath: String? = null,
    ): FindroidCarCatalogItem =
        FindroidCarCatalogItem(
            packageId = "pkg:$itemId",
            itemId = itemId,
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
            streamUrl = if (videoPath == null) "https://example.test/video.mp4" else null,
            played = played,
            favorite = false,
            playbackPositionTicks = playbackPositionTicks,
            unplayedItemCount = null,
        )

    private companion object {
        const val ITEM_ID = "11111111-1111-1111-1111-111111111111"
        const val SERIES_ID = "22222222-2222-2222-2222-222222222222"
        const val SEASON_ID = "33333333-3333-3333-3333-333333333333"
    }
}
