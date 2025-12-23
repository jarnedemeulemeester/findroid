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
    Mixed("null"),
    Folders("folders"),
    Unknown("unknown"),
    ;

    companion object {
        val defaultValue = Unknown

        val supported = listOf(
            Movies,
            TvShows,
            BoxSets,
            Mixed,
            Folders,
        )

        fun fromString(string: String?): CollectionType {
            if (string == null) { // TODO jellyfin returns null as the collectiontype for mixed libraries. This is obviously wrong, but probably an upstream issue. Should be fixed whenever upstream fixes this
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
