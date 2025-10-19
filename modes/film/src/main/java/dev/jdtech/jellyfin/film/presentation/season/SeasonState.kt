package dev.jdtech.jellyfin.film.presentation.season

import dev.jdtech.jellyfin.models.JellyCastEpisode
import dev.jdtech.jellyfin.models.JellyCastSeason

data class SeasonState(
    val season: JellyCastSeason? = null,
    val episodes: List<JellyCastEpisode> = emptyList(),
    val error: Exception? = null,
)
