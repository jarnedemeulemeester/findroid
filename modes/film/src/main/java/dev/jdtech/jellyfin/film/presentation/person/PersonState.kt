package dev.jdtech.jellyfin.film.presentation.person

import dev.jdtech.jellyfin.models.JellyCastMovie
import dev.jdtech.jellyfin.models.JellyCastPerson
import dev.jdtech.jellyfin.models.JellyCastShow

data class PersonState(
    val person: JellyCastPerson? = null,
    val starredInMovies: List<JellyCastMovie> = emptyList(),
    val starredInShows: List<JellyCastShow> = emptyList(),
    val error: Exception? = null,
)
