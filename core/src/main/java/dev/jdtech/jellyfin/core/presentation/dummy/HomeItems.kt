package dev.jdtech.jellyfin.core.presentation.dummy

import dev.jdtech.jellyfin.models.CollectionType
import dev.jdtech.jellyfin.models.HomeItem
import dev.jdtech.jellyfin.models.HomeSection
import dev.jdtech.jellyfin.models.UiText
import dev.jdtech.jellyfin.models.View
import java.util.UUID

val dummyHomeSection = HomeItem.Section(
    HomeSection(
        id = UUID.randomUUID(),
        name = UiText.DynamicString("Continue watching"),
        items = dummyMovies + dummyEpisodes,
    ),
)

val dummyHomeView = HomeItem.ViewItem(
    View(
        id = UUID.randomUUID(),
        name = "Movies",
        items = dummyMovies,
        type = CollectionType.Movies,
    ),
)

val dummyHomeItems = listOf(dummyHomeSection, dummyHomeView)
