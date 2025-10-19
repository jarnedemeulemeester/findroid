package dev.jdtech.jellyfin.core.presentation.dummy

import dev.jdtech.jellyfin.models.JellyCastEpisode
import dev.jdtech.jellyfin.models.JellyCastImages
import dev.jdtech.jellyfin.models.JellyCastMediaStream
import dev.jdtech.jellyfin.models.JellyCastSource
import dev.jdtech.jellyfin.models.JellyCastSourceType
import org.jellyfin.sdk.model.api.MediaStreamType
import java.time.LocalDateTime
import java.util.UUID

val dummyEpisode = JellyCastEpisode(
    id = UUID.randomUUID(),
    name = "Mother and Children",
    originalTitle = null,
    overview = "Stories are lies meant to entertain, and idols lie to fans eager to believe. This is Ai’s story. It is a lie, but it is also true.",
    indexNumber = 1,
    indexNumberEnd = null,
    parentIndexNumber = 1,
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
    played = true,
    favorite = true,
    canPlay = true,
    canDownload = true,
    runtimeTicks = 20L,
    playbackPositionTicks = 1200000000L,
    premiereDate = LocalDateTime.parse("2019-02-14T00:00:00"),
    seriesName = "Oshi no Ko",
    seriesId = UUID.randomUUID(),
    seasonId = UUID.randomUUID(),
    communityRating = 9.2f,
    people = emptyList(),
    trailer = null,
    images = JellyCastImages(),
    chapters = emptyList(),
    trickplayInfo = null,
)

val dummyEpisodes = listOf(
    dummyEpisode,
)
