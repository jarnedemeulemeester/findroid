package dev.jdtech.jellyfin.ui.dummy

import dev.jdtech.jellyfin.models.FindroidImages
import dev.jdtech.jellyfin.models.FindroidShow
import java.time.LocalDateTime
import java.util.UUID

val dummyShow = FindroidShow(
    id = UUID.randomUUID(),
    name = "Attack on Titan",
    originalTitle = null,
    overview = "After his hometown is destroyed and his mother is killed, young Eren Yeager vows to cleanse the earth of the giant humanoid Titans that have brought humanity to the brink of extinction.",
    sources = emptyList(),
    played = false,
    favorite = false,
    canPlay = true,
    canDownload = false,
    runtimeTicks = 0L,
    communityRating = 8.8f,
    endDate = LocalDateTime.parse("2023-11-04T00:00:00"),
    genres = listOf("Action", "Sience Fiction", "Adventure"),
    images = FindroidImages(),
    officialRating = "TV-MA",
    people = emptyList(),
    productionYear = 2013,
    seasons = emptyList(),
    status = "Ended",
    trailer = null,
    unplayedItemCount = 20,
)
