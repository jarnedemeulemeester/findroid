package dev.jdtech.jellyfin.repository

import androidx.paging.PagingData
import dev.jdtech.jellyfin.connectivity.ConnectivityMonitor
import dev.jdtech.jellyfin.connectivity.ConnectivityState
import dev.jdtech.jellyfin.models.FindroidCollection
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.FindroidMovie
import dev.jdtech.jellyfin.models.FindroidPerson
import dev.jdtech.jellyfin.models.FindroidSeason
import dev.jdtech.jellyfin.models.FindroidSegment
import dev.jdtech.jellyfin.models.FindroidShow
import dev.jdtech.jellyfin.models.FindroidSource
import dev.jdtech.jellyfin.models.SortBy
import dev.jdtech.jellyfin.models.SortOrder
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemFields
import org.jellyfin.sdk.model.api.PublicSystemInfo
import org.jellyfin.sdk.model.api.UserConfiguration

class JellyfinRepositoryRouter(
    private val onlineRepository: JellyfinRepository,
    private val offlineRepository: JellyfinRepository,
    private val connectivityMonitor: ConnectivityMonitor,
) : JellyfinRepository {
    private fun repository(): JellyfinRepository =
        if (connectivityMonitor.state.value != ConnectivityState.Online) {
            offlineRepository
        } else {
            onlineRepository
        }

    override suspend fun getPublicSystemInfo(): PublicSystemInfo = repository().getPublicSystemInfo()

    override suspend fun getUserViews(): List<BaseItemDto> = repository().getUserViews()

    override suspend fun getEpisode(itemId: UUID): FindroidEpisode = repository().getEpisode(itemId)

    override suspend fun getMovie(itemId: UUID): FindroidMovie = repository().getMovie(itemId)

    override suspend fun getShow(itemId: UUID): FindroidShow = repository().getShow(itemId)

    override suspend fun getSeason(itemId: UUID): FindroidSeason = repository().getSeason(itemId)

    override suspend fun getLibraries(): List<FindroidCollection> = repository().getLibraries()

    override suspend fun getItem(itemId: UUID): FindroidItem? = repository().getItem(itemId)

    override suspend fun getItems(
        parentId: UUID?,
        includeTypes: List<BaseItemKind>?,
        recursive: Boolean,
        sortBy: SortBy,
        sortOrder: SortOrder,
        startIndex: Int?,
        limit: Int?,
    ): List<FindroidItem> =
        repository().getItems(parentId, includeTypes, recursive, sortBy, sortOrder, startIndex, limit)

    override suspend fun getItemsPaging(
        parentId: UUID?,
        includeTypes: List<BaseItemKind>?,
        recursive: Boolean,
        sortBy: SortBy,
        sortOrder: SortOrder,
    ): Flow<PagingData<FindroidItem>> =
        repository().getItemsPaging(parentId, includeTypes, recursive, sortBy, sortOrder)

    override suspend fun getPerson(personId: UUID): FindroidPerson = repository().getPerson(personId)

    override suspend fun getPersonItems(
        personIds: List<UUID>,
        includeTypes: List<BaseItemKind>?,
        recursive: Boolean,
    ): List<FindroidItem> = repository().getPersonItems(personIds, includeTypes, recursive)

    override suspend fun getFavoriteItems(): List<FindroidItem> = repository().getFavoriteItems()

    override suspend fun getSearchItems(query: String): List<FindroidItem> = repository().getSearchItems(query)

    override suspend fun getSuggestions(): List<FindroidItem> = repository().getSuggestions()

    override suspend fun getResumeItems(): List<FindroidItem> = repository().getResumeItems()

    override suspend fun getLatestMedia(parentId: UUID): List<FindroidItem> = repository().getLatestMedia(parentId)

    override suspend fun getSeasons(seriesId: UUID, offline: Boolean): List<FindroidSeason> =
        repository().getSeasons(seriesId, offline)

    override suspend fun getNextUp(seriesId: UUID?): List<FindroidEpisode> = repository().getNextUp(seriesId)

    override suspend fun getEpisodes(
        seriesId: UUID,
        seasonId: UUID,
        fields: List<ItemFields>?,
        startItemId: UUID?,
        limit: Int?,
        offline: Boolean,
    ): List<FindroidEpisode> =
        repository().getEpisodes(seriesId, seasonId, fields, startItemId, limit, offline)

    override suspend fun getMediaSources(itemId: UUID, includePath: Boolean): List<FindroidSource> =
        repository().getMediaSources(itemId, includePath)

    override suspend fun getStreamUrl(itemId: UUID, mediaSourceId: String): String =
        repository().getStreamUrl(itemId, mediaSourceId)

    override suspend fun getSegments(itemId: UUID): List<FindroidSegment> = repository().getSegments(itemId)

    override suspend fun getTrickplayData(itemId: UUID, width: Int, index: Int): ByteArray? =
        repository().getTrickplayData(itemId, width, index)

    override suspend fun postCapabilities() = repository().postCapabilities()

    override suspend fun postPlaybackStart(itemId: UUID) = repository().postPlaybackStart(itemId)

    override suspend fun postPlaybackStop(itemId: UUID, positionTicks: Long, playedPercentage: Int) =
        repository().postPlaybackStop(itemId, positionTicks, playedPercentage)

    override suspend fun postPlaybackProgress(itemId: UUID, positionTicks: Long, isPaused: Boolean) =
        repository().postPlaybackProgress(itemId, positionTicks, isPaused)

    override suspend fun markAsFavorite(itemId: UUID) = repository().markAsFavorite(itemId)

    override suspend fun unmarkAsFavorite(itemId: UUID) = repository().unmarkAsFavorite(itemId)

    override suspend fun markAsPlayed(itemId: UUID) = repository().markAsPlayed(itemId)

    override suspend fun markAsUnplayed(itemId: UUID) = repository().markAsUnplayed(itemId)

    override fun getBaseUrl(): String = repository().getBaseUrl()

    override suspend fun updateDeviceName(name: String) = repository().updateDeviceName(name)

    override suspend fun getUserConfiguration(): UserConfiguration? = repository().getUserConfiguration()

    override suspend fun getDownloads(): List<FindroidItem> = repository().getDownloads()

    override fun getUserId(): UUID = repository().getUserId()
}
