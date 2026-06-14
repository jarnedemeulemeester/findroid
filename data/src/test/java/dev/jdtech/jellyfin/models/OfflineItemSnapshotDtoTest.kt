package dev.jdtech.jellyfin.models

import dev.jdtech.jellyfin.offline.download.OfflineItemKind
import dev.jdtech.jellyfin.offline.download.OfflineItemSnapshot
import org.junit.Assert.assertEquals
import org.junit.Test

class OfflineItemSnapshotDtoTest {
    @Test
    fun snapshotRoundTripPreservesEpisodeCatalogFields() {
        val snapshot =
            OfflineItemSnapshot(
                packageId = "pkg-1",
                serverId = "server-1",
                itemId = "1d0aaf38-82e3-29c5-d035-22592fbfa27e",
                itemKind = OfflineItemKind.EPISODE,
                name = "Стадии принятия тимлида",
                originalTitle = null,
                overview = "overview",
                runtimeTicks = 1_943_280_000L,
                playbackPositionTicks = 120_000_000L,
                played = false,
                favorite = true,
                seriesId = "0ba2d8df-ce1d-ea07-6cf5-e3ff12e08b4b",
                seriesName = "AvitoTech",
                seasonId = "11111111-1111-1111-1111-111111111111",
                seasonName = "Сезон 26",
                indexNumber = 1,
                indexNumberEnd = null,
                parentIndexNumber = 26,
                communityRating = 4.5f,
                createdAtMillis = 10,
                updatedAtMillis = 20,
            )

        val roundTrip = snapshot.toOfflineItemSnapshotDto().toOfflineItemSnapshot()

        assertEquals(snapshot, roundTrip)
    }
}
