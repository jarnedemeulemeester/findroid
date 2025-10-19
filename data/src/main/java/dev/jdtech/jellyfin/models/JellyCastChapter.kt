package dev.jdtech.jellyfin.models

import kotlinx.serialization.Serializable
import org.jellyfin.sdk.model.api.BaseItemDto

@Serializable
data class JellyCastChapter(
    /**
     * The start position.
     */
    val startPosition: Long,
    /**
     * The name.
     */
    val name: String? = null,
)

fun BaseItemDto.toJellyCastChapters(): List<JellyCastChapter> {
    return chapters?.map { chapter ->
        JellyCastChapter(
            startPosition = chapter.startPositionTicks / 10000,
            name = chapter.name,
        )
    } ?: emptyList()
}
