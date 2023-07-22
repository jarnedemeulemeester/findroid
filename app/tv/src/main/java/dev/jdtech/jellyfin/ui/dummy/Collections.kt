package dev.jdtech.jellyfin.ui.dummy

import dev.jdtech.jellyfin.models.CollectionType
import dev.jdtech.jellyfin.models.FindroidCollection
import dev.jdtech.jellyfin.models.FindroidImages
import java.util.UUID

private val dummyMoviesCollection = FindroidCollection(
    id = UUID.randomUUID(),
    name = "Movies",
    type = CollectionType.Movies,
    images = FindroidImages(),
)

private val dummyShowsCollection = FindroidCollection(
    id = UUID.randomUUID(),
    name = "Shows",
    type = CollectionType.TvShows,
    images = FindroidImages(),
)

val dummyCollections = listOf(
    dummyMoviesCollection,
    dummyShowsCollection,
)
