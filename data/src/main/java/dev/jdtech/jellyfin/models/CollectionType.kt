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
    ;

    companion object {
        val unsupportedCollections = listOf(
            HomeVideos,
            Music,
            Playlists,
            Books,
            LiveTv,
        )
    }
}
