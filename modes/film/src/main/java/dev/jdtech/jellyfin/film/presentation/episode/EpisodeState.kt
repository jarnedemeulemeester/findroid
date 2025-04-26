package dev.jdtech.jellyfin.film.presentation.episode

import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidPerson
import dev.jdtech.jellyfin.models.VideoMetadata

data class EpisodeState(
    val episode: FindroidEpisode? = null,
    val videoMetadata: VideoMetadata? = null,
    val actors: List<FindroidPerson> = emptyList(),
    val error: Exception? = null,
)
