package dev.jdtech.jellyfin.film.presentation.movie

import dev.jdtech.jellyfin.models.JellyCastItemPerson
import dev.jdtech.jellyfin.models.JellyCastMovie
import dev.jdtech.jellyfin.models.VideoMetadata

data class MovieState(
    val movie: JellyCastMovie? = null,
    val videoMetadata: VideoMetadata? = null,
    val actors: List<JellyCastItemPerson> = emptyList(),
    val director: JellyCastItemPerson? = null,
    val writers: List<JellyCastItemPerson> = emptyList(),
    val error: Exception? = null,
)
