package dev.jdtech.jellyfin.ui.dummy

import dev.jdtech.jellyfin.models.FindroidImages
import dev.jdtech.jellyfin.models.FindroidMediaStream
import dev.jdtech.jellyfin.models.FindroidMovie
import dev.jdtech.jellyfin.models.FindroidSource
import dev.jdtech.jellyfin.models.FindroidSourceType
import org.jellyfin.sdk.model.api.MediaStreamType
import java.time.LocalDateTime
import java.util.UUID

val dummyMovie = FindroidMovie(
    id = UUID.randomUUID(),
    name = "Alita: Battle Angel",
    originalTitle = null,
    overview = "When Alita awakens with no memory of who she is in a future world she does not recognize, she is taken in by Ido, a compassionate doctor who realizes that somewhere in this abandoned cyborg shell is the heart and soul of a young woman with an extraordinary past.",
    sources = listOf(
        FindroidSource(
            id = "",
            name = "",
            type = FindroidSourceType.REMOTE,
            path = "",
            size = 0L,
            mediaStreams = listOf(
                FindroidMediaStream(
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
    images = FindroidImages(),
)

val dummyMovies = listOf(
    dummyMovie,
)
