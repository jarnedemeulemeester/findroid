package dev.jdtech.jellyfin.car

import dev.jdtech.jellyfin.models.FindroidEpisode
import java.util.UUID

internal fun FindroidEpisode.belongsToOnlineSeason(
    expectedSeriesId: UUID,
    expectedSeasonId: UUID,
    expectedSeasonIndex: Int?,
): Boolean {
    if (seriesId != expectedSeriesId) return false
    return seasonId == expectedSeasonId ||
        (expectedSeasonIndex != null && parentIndexNumber == expectedSeasonIndex)
}
