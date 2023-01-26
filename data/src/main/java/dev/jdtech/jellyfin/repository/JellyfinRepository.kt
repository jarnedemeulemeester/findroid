package dev.jdtech.jellyfin.repository

import androidx.paging.PagingData
import dev.jdtech.jellyfin.models.Intro
import dev.jdtech.jellyfin.models.SortBy
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemFields
import org.jellyfin.sdk.model.api.MediaSourceInfo
import org.jellyfin.sdk.model.api.SortOrder

interface JellyfinRepository {
    suspend fun getUserViews(): List<BaseItemDto>

    suspend fun getItem(itemId: UUID): BaseItemDto

    suspend fun getItems(
        parentId: UUID? = null,
        includeTypes: List<BaseItemKind>? = null,
        recursive: Boolean = false,
        sortBy: SortBy = SortBy.defaultValue,
        sortOrder: SortOrder = SortOrder.ASCENDING
    ): List<BaseItemDto>

    suspend fun getItemsPaging(
        parentId: UUID? = null,
        includeTypes: List<BaseItemKind>? = null,
        recursive: Boolean = false,
        sortBy: SortBy = SortBy.defaultValue,
        sortOrder: SortOrder = SortOrder.ASCENDING
    ): Flow<PagingData<BaseItemDto>>

    suspend fun getPersonItems(
        personIds: List<UUID>,
        includeTypes: List<BaseItemKind>? = null,
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

    suspend fun getIntroTimestamps(itemId: UUID): Intro?

    suspend fun postCapabilities()

    suspend fun postPlaybackStart(itemId: UUID)

    suspend fun postPlaybackStop(itemId: UUID, positionTicks: Long)

    suspend fun postPlaybackProgress(itemId: UUID, positionTicks: Long, isPaused: Boolean)

    suspend fun markAsFavorite(itemId: UUID)

    suspend fun unmarkAsFavorite(itemId: UUID)

    suspend fun markAsPlayed(itemId: UUID)

    suspend fun markAsUnplayed(itemId: UUID)

    suspend fun getIntros(itemId: UUID): List<BaseItemDto>

    fun getBaseUrl(): String

    suspend fun updateDeviceName(name: String)
}
