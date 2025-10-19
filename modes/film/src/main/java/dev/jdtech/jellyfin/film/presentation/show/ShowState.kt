package dev.jdtech.jellyfin.film.presentation.show

import dev.jdtech.jellyfin.models.JellyCastEpisode
import dev.jdtech.jellyfin.models.JellyCastItemPerson
import dev.jdtech.jellyfin.models.JellyCastSeason
import dev.jdtech.jellyfin.models.JellyCastShow

data class ShowState(
    val show: JellyCastShow? = null,
    val nextUp: JellyCastEpisode? = null,
    val seasons: List<JellyCastSeason> = emptyList(),
    val actors: List<JellyCastItemPerson> = emptyList(),
    val director: JellyCastItemPerson? = null,
    val writers: List<JellyCastItemPerson> = emptyList(),
    val error: Exception? = null,
)
