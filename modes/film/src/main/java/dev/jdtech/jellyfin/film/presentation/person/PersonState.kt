package dev.jdtech.jellyfin.film.presentation.person

import dev.jdtech.jellyfin.models.FindroidMovie
import dev.jdtech.jellyfin.models.FindroidPerson
import dev.jdtech.jellyfin.models.FindroidShow

data class PersonState(
    val person: FindroidPerson? = null,
    val starredInMovies: List<FindroidMovie> = emptyList(),
    val starredInShows: List<FindroidShow> = emptyList(),
    val error: Exception? = null,
)
