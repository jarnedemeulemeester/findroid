package dev.jdtech.jellyfin.repository

import dev.jdtech.jellyfin.models.ContentType
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.ItemFields
import org.jellyfin.sdk.model.api.MediaSourceInfo
import java.util.*

interface JellyfinRepository {
    suspend fun getUserViews(): List<BaseItemDto>

    suspend fun getItem(itemId: UUID): BaseItemDto

    suspend fun getItems(
        parentId: UUID? = null,
        includeTypes: List<String>? = null,
        recursive: Boolean = false
    ): List<BaseItemDto>

    suspend fun getPersonItems(
        personIds: List<UUID>,
        includeTypes: List<ContentType>? = null,
        recursive: Boolean = true
    ): List<BaseItemDto>

    suspend fun getFavoriteItems(): List<BaseItemDto>

    suspend fun getSearchItems(searchQuery: String): List<BaseItemDto>

    suspend fun getResumeItems(): List<BaseItemDto>

    suspend fun getLatestMedia(parentId: UUID): List<BaseItemDto>

    suspend fun getSeasons(seriesId: UUID): List<BaseItemDto>

    suspend fun getNextUp(seriesId: UUID? = null): List<BaseItemDto>

    suspend fun getEpisodes(
        seriesId: UUID,
        seasonId: UUID,
        fields: List<ItemFields>? = null,
        startItemId: UUID? = null
    ): List<BaseItemDto>

    suspend fun getMediaSources(itemId: UUID): List<MediaSourceInfo>

    suspend fun getStreamUrl(itemId: UUID, mediaSourceId: String): String

    suspend fun postCapabilities()

    suspend fun postPlaybackStart(itemId: UUID)

    suspend fun postPlaybackStop(itemId: UUID, positionTicks: Long)

    suspend fun postPlaybackProgress(itemId: UUID, positionTicks: Long, isPaused: Boolean)

    suspend fun markAsFavorite(itemId: UUID)

    suspend fun unmarkAsFavorite(itemId: UUID)

    suspend fun markAsPlayed(itemId: UUID)

    suspend fun markAsUnplayed(itemId: UUID)

    suspend fun getIntros(itemId: UUID): List<BaseItemDto>
}