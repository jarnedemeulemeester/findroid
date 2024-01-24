package dev.jdtech.jellyfin.models

import org.jellyfin.sdk.model.api.BaseItemDto

data class FindroidChapter(
    /**
     * The start position ticks.
     */
    val startPositionTicks: Long,
    /**
     * The name.
     */
    val name: String? = null,
)

fun BaseItemDto.toFindroidChapters(): List<FindroidChapter>? {
    return chapters?.map { chapter ->
        FindroidChapter(
            startPositionTicks = chapter.startPositionTicks,
            name = chapter.name,
        )
    }
}
