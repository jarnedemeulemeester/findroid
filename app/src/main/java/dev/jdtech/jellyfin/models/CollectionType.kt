package dev.jdtech.jellyfin.models

import dev.jdtech.jellyfin.models.CollectionType.Books
import dev.jdtech.jellyfin.models.CollectionType.HomeVideos
import dev.jdtech.jellyfin.models.CollectionType.LiveTv
import dev.jdtech.jellyfin.models.CollectionType.Music
import dev.jdtech.jellyfin.models.CollectionType.Playlists
import dev.jdtech.jellyfin.models.CollectionType.BoxSets

enum class CollectionType (val type: String) {
    HomeVideos("homevideos"),
    Music("music"),
    Playlists("playlists"),
    Books("books"),
    LiveTv("livetv"),
    BoxSets("boxsets")
}

fun unsupportedCollections() = listOf(
    HomeVideos, Music, Playlists, Books, LiveTv, BoxSets
)