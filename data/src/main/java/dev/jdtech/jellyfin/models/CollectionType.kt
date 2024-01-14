package dev.jdtech.jellyfin.models

enum class CollectionType(val type: String) {
    Movies("movies"),
    TvShows("tvshows"),
    HomeVideos("homevideos"),
    Music("music"),
    Playlists("playlists"),
    Books("books"),
    LiveTv("livetv"),
    BoxSets("boxsets"),
    Mixed("mixed"),
    Unknown("unknown"),
    ;

    companion object {
        val defaultValue = Unknown

        val supported = listOf(
            Movies,
            TvShows,
            BoxSets,
            Mixed,
        )

        fun fromString(string: String?): CollectionType {
            if (string == null) {
                return Mixed
            }

            return try {
                entries.first { it.type == string }
            } catch (e: NoSuchElementException) {
                defaultValue
            }
        }
    }
}
