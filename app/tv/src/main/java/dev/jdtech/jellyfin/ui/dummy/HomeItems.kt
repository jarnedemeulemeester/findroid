package dev.jdtech.jellyfin.ui.dummy

import dev.jdtech.jellyfin.models.CollectionType
import dev.jdtech.jellyfin.models.HomeItem
import dev.jdtech.jellyfin.models.HomeSection
import dev.jdtech.jellyfin.models.UiText
import dev.jdtech.jellyfin.models.View
import java.util.UUID

val dummyHomeItems = listOf(
    HomeItem.Section(
        HomeSection(
            id = UUID.randomUUID(),
            name = UiText.DynamicString("Continue watching"),
            items = dummyMovies + dummyEpisodes,
        ),
    ),
    HomeItem.ViewItem(
        View(
            id = UUID.randomUUID(),
            name = "Movies",
            items = dummyMovies,
            type = CollectionType.Movies,
        ),
    ),
)
