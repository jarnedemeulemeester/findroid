package dev.jdtech.jellyfin.models

enum class SortBy(val SortString: String) {
    NAME("SortName"),
    IMDB_RATING("CommunityRating"),
    PARENTAL_RATING("CriticRating"),
    DATE_ADDED("DateCreated"),
    DATE_PLAYED("DatePlayed"),
    RELEASE_DATE("PremiereDate"),
    ;

    companion object {
        val defaultValue = NAME

        fun fromString(string: String): SortBy {
            return try {
                valueOf(string)
            } catch (e: IllegalArgumentException) {
                defaultValue
            }
        }
    }
}
