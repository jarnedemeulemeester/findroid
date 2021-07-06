package dev.jdtech.jellyfin.repository

import org.jellyfin.sdk.model.api.BaseItemDto
import java.util.*

interface JellyfinRepository {
    suspend fun getItem(itemId: UUID): BaseItemDto
}