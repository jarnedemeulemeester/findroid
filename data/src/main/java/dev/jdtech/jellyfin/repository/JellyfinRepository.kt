package dev.jdtech.jellyfin.repository

import androidx.paging.PagingData
import dev.jdtech.jellyfin.models.JellyCastCollection
import dev.jdtech.jellyfin.models.JellyCastEpisode
import dev.jdtech.jellyfin.models.JellyCastItem
import dev.jdtech.jellyfin.models.JellyCastMovie
import dev.jdtech.jellyfin.models.JellyCastPerson
import dev.jdtech.jellyfin.models.JellyCastSeason
import dev.jdtech.jellyfin.models.JellyCastSegment
import dev.jdtech.jellyfin.models.JellyCastShow
import dev.jdtech.jellyfin.models.JellyCastSource
import dev.jdtech.jellyfin.models.SortBy
import kotlinx.coroutines.flow.Flow
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemFields
import org.jellyfin.sdk.model.api.PublicSystemInfo
import org.jellyfin.sdk.model.api.SortOrder
import org.jellyfin.sdk.model.api.UserConfiguration
import java.util.UUID

interface JellyfinRepository {
    suspend fun getPublicSystemInfo(): PublicSystemInfo

    suspend fun getUserViews(): List<BaseItemDto>

    suspend fun getEpisode(itemId: UUID): JellyCastEpisode
    suspend fun getMovie(itemId: UUID): JellyCastMovie

    suspend fun getShow(itemId: UUID): JellyCastShow

    suspend fun getSeason(itemId: UUID): JellyCastSeason

    suspend fun getLibraries(): List<JellyCastCollection>

    suspend fun getItems(
        parentId: UUID? = null,
        includeTypes: List<BaseItemKind>? = null,
        recursive: Boolean = false,
        sortBy: SortBy = SortBy.defaultValue,
        sortOrder: SortOrder = SortOrder.ASCENDING,
        startIndex: Int? = null,
        limit: Int? = null,
    ): List<JellyCastItem>

    suspend fun getItemsPaging(
        parentId: UUID? = null,
        includeTypes: List<BaseItemKind>? = null,
        recursive: Boolean = false,
        sortBy: SortBy = SortBy.defaultValue,
        sortOrder: SortOrder = SortOrder.ASCENDING,
    ): Flow<PagingData<JellyCastItem>>

    suspend fun getPerson(
        personId: UUID,
    ): JellyCastPerson

    suspend fun getPersonItems(
        personIds: List<UUID>,
        includeTypes: List<BaseItemKind>? = null,
        recursive: Boolean = true,
    ): List<JellyCastItem>

    suspend fun getFavoriteItems(): List<JellyCastItem>

    suspend fun getSearchItems(query: String): List<JellyCastItem>

    suspend fun getSuggestions(): List<JellyCastItem>

    suspend fun getResumeItems(): List<JellyCastItem>

    suspend fun getLatestMedia(parentId: UUID): List<JellyCastItem>

    suspend fun getSeasons(seriesId: UUID, offline: Boolean = false): List<JellyCastSeason>

    suspend fun getNextUp(seriesId: UUID? = null): List<JellyCastEpisode>

    suspend fun getEpisodes(
        seriesId: UUID,
        seasonId: UUID,
        fields: List<ItemFields>? = null,
        startItemId: UUID? = null,
        limit: Int? = null,
        offline: Boolean = false,
    ): List<JellyCastEpisode>

    suspend fun getMediaSources(itemId: UUID, includePath: Boolean = false): List<JellyCastSource>

    suspend fun getStreamUrl(itemId: UUID, mediaSourceId: String): String

    suspend fun getSegments(itemId: UUID): List<JellyCastSegment>

    suspend fun getTrickplayData(itemId: UUID, width: Int, index: Int): ByteArray?

    suspend fun postCapabilities()

    suspend fun postPlaybackStart(itemId: UUID)

    suspend fun postPlaybackStop(itemId: UUID, positionTicks: Long, playedPercentage: Int)

    suspend fun postPlaybackProgress(itemId: UUID, positionTicks: Long, isPaused: Boolean)

    suspend fun markAsFavorite(itemId: UUID)

    suspend fun unmarkAsFavorite(itemId: UUID)

    suspend fun markAsPlayed(itemId: UUID)

    suspend fun markAsUnplayed(itemId: UUID)

    fun getBaseUrl(): String

    suspend fun updateDeviceName(name: String)

    suspend fun getUserConfiguration(): UserConfiguration?

    suspend fun getDownloads(): List<JellyCastItem>

    fun getUserId(): UUID
}
