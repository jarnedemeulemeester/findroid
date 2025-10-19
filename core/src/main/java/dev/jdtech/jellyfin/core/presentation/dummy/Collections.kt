package dev.jdtech.jellyfin.core.presentation.dummy

import dev.jdtech.jellyfin.models.CollectionType
import dev.jdtech.jellyfin.models.JellyCastCollection
import dev.jdtech.jellyfin.models.JellyCastImages
import java.util.UUID

private val dummyMoviesCollection = JellyCastCollection(
    id = UUID.randomUUID(),
    name = "Movies",
    type = CollectionType.Movies,
    images = JellyCastImages(),
)

private val dummyShowsCollection = JellyCastCollection(
    id = UUID.randomUUID(),
    name = "Shows",
    type = CollectionType.TvShows,
    images = JellyCastImages(),
)

val dummyCollections = listOf(
    dummyMoviesCollection,
    dummyShowsCollection,
)
