package dev.jdtech.jellyfin.models

import org.jellyfin.sdk.model.api.BaseItemDto
import java.util.UUID

sealed class StarredIn {
    abstract val id: UUID
    abstract val title: String
    abstract val released: String
    abstract val dto: BaseItemDto

    data class Movie(
        override val id: UUID,
        override val title: String,
        override val released: String,
        override val dto: BaseItemDto
    ) : StarredIn()

    data class Show(
        override val id: UUID,
        override val title: String,
        override val released: String,
        override val dto: BaseItemDto
    ) : StarredIn()
}
