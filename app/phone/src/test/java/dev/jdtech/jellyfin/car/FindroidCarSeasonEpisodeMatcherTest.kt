package dev.jdtech.jellyfin.car

import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidImages
import java.util.UUID
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FindroidCarSeasonEpisodeMatcherTest {
    @Test
    fun matchesEpisodeBySeasonId() {
        val episode = episode(seasonId = SEASON_ID, parentIndexNumber = 17)

        assertTrue(
            episode.belongsToOnlineSeason(
                expectedSeriesId = SERIES_ID,
                expectedSeasonId = SEASON_ID,
                expectedSeasonIndex = 18,
            )
        )
    }

    @Test
    fun matchesEpisodeBySeasonIndexWhenSeasonIdDiffers() {
        val episode = episode(seasonId = OTHER_SEASON_ID, parentIndexNumber = 18)

        assertTrue(
            episode.belongsToOnlineSeason(
                expectedSeriesId = SERIES_ID,
                expectedSeasonId = SEASON_ID,
                expectedSeasonIndex = 18,
            )
        )
    }

    @Test
    fun rejectsSameSeasonIndexFromAnotherSeries() {
        val episode =
            episode(seriesId = OTHER_SERIES_ID, seasonId = OTHER_SEASON_ID, parentIndexNumber = 18)

        assertFalse(
            episode.belongsToOnlineSeason(
                expectedSeriesId = SERIES_ID,
                expectedSeasonId = SEASON_ID,
                expectedSeasonIndex = 18,
            )
        )
    }

    @Test
    fun rejectsDifferentSeasonWithoutIndexFallback() {
        val episode = episode(seasonId = OTHER_SEASON_ID, parentIndexNumber = 17)

        assertFalse(
            episode.belongsToOnlineSeason(
                expectedSeriesId = SERIES_ID,
                expectedSeasonId = SEASON_ID,
                expectedSeasonIndex = null,
            )
        )
    }

    private fun episode(
        seriesId: UUID = SERIES_ID,
        seasonId: UUID = SEASON_ID,
        parentIndexNumber: Int = 18,
    ): FindroidEpisode =
        FindroidEpisode(
            id = EPISODE_ID,
            name = "Episode",
            originalTitle = null,
            overview = "",
            indexNumber = 1,
            indexNumberEnd = null,
            parentIndexNumber = parentIndexNumber,
            sources = emptyList(),
            played = false,
            favorite = false,
            canPlay = true,
            canDownload = true,
            runtimeTicks = 0,
            playbackPositionTicks = 0,
            premiereDate = null,
            seriesId = seriesId,
            seriesName = "Series",
            seasonId = seasonId,
            seasonName = "Season 18",
            communityRating = null,
            people = emptyList(),
            images = FindroidImages(),
            chapters = emptyList(),
            trickplayInfo = null,
        )

    private companion object {
        val SERIES_ID: UUID = UUID.fromString("11111111-1111-1111-1111-111111111111")
        val OTHER_SERIES_ID: UUID = UUID.fromString("22222222-2222-2222-2222-222222222222")
        val SEASON_ID: UUID = UUID.fromString("33333333-3333-3333-3333-333333333333")
        val OTHER_SEASON_ID: UUID = UUID.fromString("44444444-4444-4444-4444-444444444444")
        val EPISODE_ID: UUID = UUID.fromString("55555555-5555-5555-5555-555555555555")
    }
}
