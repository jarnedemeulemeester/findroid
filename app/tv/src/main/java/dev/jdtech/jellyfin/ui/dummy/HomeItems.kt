package dev.jdtech.jellyfin.ui.dummy

import dev.jdtech.jellyfin.models.HomeItem
import dev.jdtech.jellyfin.models.HomeSection
import dev.jdtech.jellyfin.models.UiText
import java.util.UUID

val dummyHomeItems = listOf(
    HomeItem.Libraries(
        HomeSection(
            id = UUID.randomUUID(),
            name = UiText.DynamicString("Libraries"),
            items = dummyCollections,
        ),
    ),
    HomeItem.Section(
        HomeSection(
            id = UUID.randomUUID(),
            name = UiText.DynamicString("Continue watching"),
            items = dummyMovies,
        ),
    ),
)
