package dev.jdtech.jellyfin.models

import dev.jdtech.jellyfin.repository.JellyfinRepository
import java.util.UUID
import org.jellyfin.sdk.model.api.BaseItemDto

data class FindroidPerson(
    val id: UUID,
    val name: String,
    val overview: String,
    val images: FindroidImages,
)

fun BaseItemDto.toFindroidPerson(repository: JellyfinRepository): FindroidPerson {
    return FindroidPerson(
        id = id,
        name = name.orEmpty(),
        overview = overview.orEmpty(),
        images = toFindroidImages(repository),
    )
}
