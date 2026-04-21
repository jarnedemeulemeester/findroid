package dev.jdtech.jellyfin.models

enum class SortBy(val sortString: String) {
    NAME("SortName"),
    IMDB_RATING("CommunityRating"),
    PARENTAL_RATING("CriticRating"),
    DATE_ADDED("DateCreated"),
    DATE_PLAYED("DatePlayed"),
    RELEASE_DATE("PremiereDate"),
    SERIES_DATE_PLAYED("SeriesDatePlayed");

    companion object {
        val defaultValue = NAME

        fun fromString(string: String): SortBy {
            return try {
                valueOf(string)
            } catch (_: IllegalArgumentException) {
                defaultValue
            }
        }
    }
}
