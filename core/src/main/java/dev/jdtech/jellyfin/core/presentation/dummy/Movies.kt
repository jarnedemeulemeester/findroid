package dev.jdtech.jellyfin.core.presentation.dummy

import dev.jdtech.jellyfin.models.JellyCastImages
import dev.jdtech.jellyfin.models.JellyCastMediaStream
import dev.jdtech.jellyfin.models.JellyCastMovie
import dev.jdtech.jellyfin.models.JellyCastSource
import dev.jdtech.jellyfin.models.JellyCastSourceType
import org.jellyfin.sdk.model.api.MediaStreamType
import java.time.LocalDateTime
import java.util.UUID

val dummyMovie = JellyCastMovie(
    id = UUID.randomUUID(),
    name = "Alita: Battle Angel",
    originalTitle = null,
    overview = "When Alita awakens with no memory of who she is in a future world she does not recognize, she is taken in by Ido, a compassionate doctor who realizes that somewhere in this abandoned cyborg shell is the heart and soul of a young woman with an extraordinary past.",
    sources = listOf(
        JellyCastSource(
            id = "",
            name = "",
            type = JellyCastSourceType.REMOTE,
            path = "",
            size = 0L,
            mediaStreams = listOf(
                JellyCastMediaStream(
                    title = "",
                    displayTitle = "",
                    language = "en",
                    type = MediaStreamType.VIDEO,
                    codec = "hevc",
                    isExternal = false,
                    path = "",
                    channelLayout = null,
                    videoRangeType = null,
                    height = 1080,
                    width = 1920,
                    videoDoViTitle = null,
                ),
            ),
        ),
    ),
    played = false,
    favorite = true,
    canPlay = true,
    canDownload = true,
    runtimeTicks = 20L,
    playbackPositionTicks = 15L,
    premiereDate = LocalDateTime.parse("2019-02-14T00:00:00"),
    people = emptyList(),
    genres = listOf("Action", "Sience Fiction", "Adventure"),
    communityRating = 7.2f,
    officialRating = "PG-13",
    status = "Ended",
    productionYear = 2019,
    endDate = null,
    trailer = "https://www.youtube.com/watch?v=puKWa8hrvA8",
    images = JellyCastImages(),
    chapters = emptyList(),
    trickplayInfo = null,
)

val dummyMovies = listOf(
    dummyMovie,
)
