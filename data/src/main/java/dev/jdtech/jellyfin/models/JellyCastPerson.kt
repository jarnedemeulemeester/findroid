package dev.jdtech.jellyfin.models

import dev.jdtech.jellyfin.repository.JellyfinRepository
import org.jellyfin.sdk.model.api.BaseItemDto
import java.util.UUID

data class JellyCastPerson(
    val id: UUID,
    val name: String,
    val overview: String,
    val images: JellyCastImages,
)

fun BaseItemDto.toJellyCastPerson(
    repository: JellyfinRepository,
): JellyCastPerson {
    return JellyCastPerson(
        id = id,
        name = name.orEmpty(),
        overview = overview.orEmpty(),
        images = toJellyCastImages(repository),
    )
}
