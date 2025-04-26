package dev.jdtech.jellyfin.film.presentation.movie

import dev.jdtech.jellyfin.models.FindroidMovie
import dev.jdtech.jellyfin.models.FindroidPerson
import dev.jdtech.jellyfin.models.VideoMetadata

data class MovieState(
    val movie: FindroidMovie? = null,
    val videoMetadata: VideoMetadata? = null,
    val actors: List<FindroidPerson> = emptyList(),
    val director: FindroidPerson? = null,
    val writers: List<FindroidPerson> = emptyList(),
    val error: Exception? = null,
)
