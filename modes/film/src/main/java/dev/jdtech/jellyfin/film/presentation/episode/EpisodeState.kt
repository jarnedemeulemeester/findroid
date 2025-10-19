package dev.jdtech.jellyfin.film.presentation.episode

import dev.jdtech.jellyfin.models.JellyCastEpisode
import dev.jdtech.jellyfin.models.JellyCastItemPerson
import dev.jdtech.jellyfin.models.VideoMetadata

data class EpisodeState(
    val episode: JellyCastEpisode? = null,
    val videoMetadata: VideoMetadata? = null,
    val actors: List<JellyCastItemPerson> = emptyList(),
    val error: Exception? = null,
)
