package dev.jdtech.jellyfin.film.presentation.library

import dev.jdtech.jellyfin.models.SortBy

enum class LibrarySortBy(val repositorySortBy: SortBy, val displaysEpisodes: Boolean = false) {
    NAME(SortBy.NAME),
    IMDB_RATING(SortBy.IMDB_RATING),
    PARENTAL_RATING(SortBy.PARENTAL_RATING),
    DATE_ADDED(SortBy.DATE_ADDED),
    DATE_PLAYED(SortBy.DATE_PLAYED),
    RELEASE_DATE(SortBy.RELEASE_DATE),
    EPISODES_BY_DATE_ADDED(SortBy.DATE_ADDED, displaysEpisodes = true),
    EPISODES_BY_AIR_DATE(SortBy.RELEASE_DATE, displaysEpisodes = true);

    companion object {
        val defaultValue = NAME

        fun fromString(string: String): LibrarySortBy {
            return try {
                valueOf(string)
            } catch (_: IllegalArgumentException) {
                fromLegacySortBy(string)
            }
        }

        private fun fromLegacySortBy(string: String): LibrarySortBy {
            return when (SortBy.fromString(string)) {
                SortBy.NAME -> NAME
                SortBy.IMDB_RATING -> IMDB_RATING
                SortBy.PARENTAL_RATING -> PARENTAL_RATING
                SortBy.DATE_ADDED -> DATE_ADDED
                SortBy.DATE_PLAYED,
                SortBy.SERIES_DATE_PLAYED -> DATE_PLAYED
                SortBy.RELEASE_DATE -> RELEASE_DATE
            }
        }
    }
}
