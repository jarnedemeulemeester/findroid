package dev.jdtech.jellyfin.film.presentation.episode

import dev.jdtech.jellyfin.models.FindroidEpisode

data class EpisodeState(
    val episode: FindroidEpisode? = null,
    val error: Exception? = null,
)
