package dev.jdtech.jellyfin.repository

import android.content.Context
import androidx.paging.PagingData
import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.database.ServerDatabaseDao
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
import dev.jdtech.jellyfin.models.toJellyCastEpisode
import dev.jdtech.jellyfin.models.toJellyCastMovie
import dev.jdtech.jellyfin.models.toJellyCastSeason
import dev.jdtech.jellyfin.models.toJellyCastSegment
import dev.jdtech.jellyfin.models.toJellyCastShow
import dev.jdtech.jellyfin.models.toJellyCastSource
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemFields
import org.jellyfin.sdk.model.api.PublicSystemInfo
import org.jellyfin.sdk.model.api.SortOrder
import org.jellyfin.sdk.model.api.UserConfiguration
import java.io.File
import java.util.UUID

class JellyfinRepositoryOfflineImpl(
    private val context: Context,
    private val jellyfinApi: JellyfinApi,
    private val database: ServerDatabaseDao,
    private val appPreferences: AppPreferences,
) : JellyfinRepository {

    override suspend fun getPublicSystemInfo(): PublicSystemInfo {
        throw Exception("System info not available in offline mode")
    }

    override suspend fun getUserViews(): List<BaseItemDto> {
        return emptyList()
    }

    override suspend fun getMovie(itemId: UUID): JellyCastMovie =
        withContext(Dispatchers.IO) {
            database.getMovie(itemId).toJellyCastMovie(database, jellyfinApi.userId!!)
        }

    override suspend fun getShow(itemId: UUID): JellyCastShow =
        withContext(Dispatchers.IO) {
            database.getShow(itemId).toJellyCastShow(database, jellyfinApi.userId!!)
        }

    override suspend fun getSeason(itemId: UUID): JellyCastSeason =
        withContext(Dispatchers.IO) {
            database.getSeason(itemId).toJellyCastSeason(database, jellyfinApi.userId!!)
        }

    override suspend fun getEpisode(itemId: UUID): JellyCastEpisode =
        withContext(Dispatchers.IO) {
            database.getEpisode(itemId).toJellyCastEpisode(database, jellyfinApi.userId!!)
        }

    override suspend fun getLibraries(): List<JellyCastCollection> {
        // Offline mode: no remote libraries. Return empty to avoid crashes.
        return emptyList()
    }

    override suspend fun getItems(
        parentId: UUID?,
        includeTypes: List<BaseItemKind>?,
        recursive: Boolean,
        sortBy: SortBy,
        sortOrder: SortOrder,
        startIndex: Int?,
        limit: Int?,
    ): List<JellyCastItem> {
        return withContext(Dispatchers.IO) {
            val serverId = appPreferences.getValue(appPreferences.currentServer) ?: return@withContext emptyList()
            val userId = jellyfinApi.userId ?: return@withContext emptyList()

            val result = mutableListOf<JellyCastItem>()

            // Helper to apply sorting and paging
            fun List<JellyCastItem>.applySortAndPaging(): List<JellyCastItem> {
                val sorted = when (sortBy) {
                    SortBy.NAME -> this.sortedBy { it.name.lowercase() }
                    else -> this // Other sort modes not available offline; keep DB/default order
                }.let { list ->
                    if (sortOrder == SortOrder.DESCENDING) list.reversed() else list
                }
                val from = startIndex ?: 0
                val toExclusive = if (limit != null) (from + limit).coerceAtMost(sorted.size) else sorted.size
                return if (from in 0..sorted.size) sorted.subList(from, toExclusive) else emptyList()
            }

            if (includeTypes.isNullOrEmpty()) return@withContext emptyList()

            includeTypes.forEach { kind ->
                when (kind) {
                    BaseItemKind.MOVIE -> {
                        result += database.getMoviesByServerId(serverId).map { it.toJellyCastMovie(database, userId) }
                    }
                    BaseItemKind.SERIES -> {
                        result += database.getShowsByServerId(serverId).map { it.toJellyCastShow(database, userId) }
                    }
                    BaseItemKind.SEASON -> {
                        // parentId should be the seriesId
                        if (parentId != null) {
                            result += database.getSeasonsByShowId(parentId).map { it.toJellyCastSeason(database, userId) }
                        }
                    }
                    BaseItemKind.EPISODE -> {
                        // If parentId provided, assume it's a seasonId; otherwise list all episodes for server
                        val episodes = if (parentId != null) {
                            database.getEpisodesBySeasonId(parentId)
                        } else {
                            database.getEpisodesByServerId(serverId)
                        }
                        result += episodes.map { it.toJellyCastEpisode(database, userId) }
                    }
                    else -> {
                        // Not supported offline
                    }
                }
            }

            result.applySortAndPaging()
        }
    }

    override suspend fun getItemsPaging(
        parentId: UUID?,
        includeTypes: List<BaseItemKind>?,
        recursive: Boolean,
        sortBy: SortBy,
        sortOrder: SortOrder,
    ): Flow<PagingData<JellyCastItem>> {
        return ItemsPagingSource(
            this,
            parentId,
            includeTypes,
            recursive,
            sortBy,
            sortOrder,
        ).let { pagingSource ->
            // Reuse the same paging approach as online by delegating to ItemsPagingSource through Pager
            androidx.paging.Pager(
                config = androidx.paging.PagingConfig(pageSize = 10, enablePlaceholders = false),
                pagingSourceFactory = { pagingSource },
            ).flow
        }
    }

    override suspend fun getPerson(personId: UUID): JellyCastPerson {
        // Offline doesn't hold people metadata; return a minimal placeholder
        return JellyCastPerson(
            id = personId,
            name = "",
            overview = "",
            images = dev.jdtech.jellyfin.models.JellyCastImages(),
        )
    }

    override suspend fun getPersonItems(
        personIds: List<UUID>,
        includeTypes: List<BaseItemKind>?,
        recursive: Boolean,
    ): List<JellyCastItem> {
        // Not supported offline
        return emptyList()
    }

    override suspend fun getFavoriteItems(): List<JellyCastItem> {
        return withContext(Dispatchers.IO) {
            val serverId = appPreferences.getValue(appPreferences.currentServer) ?: return@withContext emptyList()
            val userId = jellyfinApi.userId ?: return@withContext emptyList()
            val items = mutableListOf<JellyCastItem>()
            items += database.getMoviesByServerId(serverId).map { it.toJellyCastMovie(database, userId) }
            items += database.getShowsByServerId(serverId).map { it.toJellyCastShow(database, userId) }
            items += database.getEpisodesByServerId(serverId).map { it.toJellyCastEpisode(database, userId) }
            items.filter { it.favorite }
        }
    }

    override suspend fun getSearchItems(query: String): List<JellyCastItem> {
        return withContext(Dispatchers.IO) {
            val movies = database.searchMovies(appPreferences.getValue(appPreferences.currentServer)!!, query).map { it.toJellyCastMovie(database, jellyfinApi.userId!!) }
            val shows = database.searchShows(appPreferences.getValue(appPreferences.currentServer)!!, query).map { it.toJellyCastShow(database, jellyfinApi.userId!!) }
            val episodes = database.searchEpisodes(appPreferences.getValue(appPreferences.currentServer)!!, query).map { it.toJellyCastEpisode(database, jellyfinApi.userId!!) }
            movies + shows + episodes
        }
    }

    override suspend fun getSuggestions(): List<JellyCastItem> {
        return emptyList()
    }

    override suspend fun getResumeItems(): List<JellyCastItem> {
        return withContext(Dispatchers.IO) {
            val movies = database.getMoviesByServerId(appPreferences.getValue(appPreferences.currentServer)!!).map { it.toJellyCastMovie(database, jellyfinApi.userId!!) }.filter { it.playbackPositionTicks > 0 }
            val episodes = database.getEpisodesByServerId(appPreferences.getValue(appPreferences.currentServer)!!).map { it.toJellyCastEpisode(database, jellyfinApi.userId!!) }.filter { it.playbackPositionTicks > 0 }
            movies + episodes
        }
    }

    override suspend fun getLatestMedia(parentId: UUID): List<JellyCastItem> {
        return emptyList()
    }

    override suspend fun getSeasons(seriesId: UUID, offline: Boolean): List<JellyCastSeason> =
        withContext(Dispatchers.IO) {
            database.getSeasonsByShowId(seriesId).map { it.toJellyCastSeason(database, jellyfinApi.userId!!) }
        }

    override suspend fun getNextUp(seriesId: UUID?): List<JellyCastEpisode> {
        return withContext(Dispatchers.IO) {
            val result = mutableListOf<JellyCastEpisode>()
            val shows = database.getShowsByServerId(appPreferences.getValue(appPreferences.currentServer)!!).filter {
                if (seriesId != null) it.id == seriesId else true
            }
            for (show in shows) {
                val episodes = database.getEpisodesByShowId(show.id).map { it.toJellyCastEpisode(database, jellyfinApi.userId!!) }
                val indexOfLastPlayed = episodes.indexOfLast { it.played }
                if (indexOfLastPlayed == -1) {
                    result.add(episodes.first())
                } else {
                    episodes.getOrNull(indexOfLastPlayed + 1)?.let { result.add(it) }
                }
            }
            result.filter { it.playbackPositionTicks == 0L }
        }
    }

    override suspend fun getEpisodes(
        seriesId: UUID,
        seasonId: UUID,
        fields: List<ItemFields>?,
        startItemId: UUID?,
        limit: Int?,
        offline: Boolean,
    ): List<JellyCastEpisode> =
        withContext(Dispatchers.IO) {
            val items = database.getEpisodesBySeasonId(seasonId).map { it.toJellyCastEpisode(database, jellyfinApi.userId!!) }
            if (startItemId != null) return@withContext items.dropWhile { it.id != startItemId }
            items
        }

    override suspend fun getMediaSources(itemId: UUID, includePath: Boolean): List<JellyCastSource> =
        withContext(Dispatchers.IO) {
            database.getSources(itemId).map { it.toJellyCastSource(database) }
        }

    override suspend fun getStreamUrl(itemId: UUID, mediaSourceId: String): String {
        // Offline streaming url isn't applicable; return empty string
        return ""
    }

    override suspend fun getSegments(itemId: UUID): List<JellyCastSegment> =
        withContext(Dispatchers.IO) {
            database.getSegments(itemId).map { it.toJellyCastSegment() }
        }

    override suspend fun getTrickplayData(itemId: UUID, width: Int, index: Int): ByteArray? =
        withContext(Dispatchers.IO) {
            try {
                val sources = File(context.filesDir, "trickplay/$itemId").listFiles() ?: return@withContext null
                File(sources.first(), index.toString()).readBytes()
            } catch (_: Exception) {
                null
            }
        }

    override suspend fun postCapabilities() {}

    override suspend fun postPlaybackStart(itemId: UUID) {}

    override suspend fun postPlaybackStop(itemId: UUID, positionTicks: Long, playedPercentage: Int) {
        withContext(Dispatchers.IO) {
            when {
                playedPercentage < 10 -> {
                    database.setPlaybackPositionTicks(itemId, jellyfinApi.userId!!, 0)
                    database.setPlayed(jellyfinApi.userId!!, itemId, false)
                }
                playedPercentage > 90 -> {
                    database.setPlaybackPositionTicks(itemId, jellyfinApi.userId!!, 0)
                    database.setPlayed(jellyfinApi.userId!!, itemId, true)
                }
                else -> {
                    database.setPlaybackPositionTicks(itemId, jellyfinApi.userId!!, positionTicks)
                    database.setPlayed(jellyfinApi.userId!!, itemId, false)
                }
            }
            database.setUserDataToBeSynced(jellyfinApi.userId!!, itemId, true)
        }
    }

    override suspend fun postPlaybackProgress(
        itemId: UUID,
        positionTicks: Long,
        isPaused: Boolean,
    ) {
        withContext(Dispatchers.IO) {
            database.setPlaybackPositionTicks(itemId, jellyfinApi.userId!!, positionTicks)
            database.setUserDataToBeSynced(jellyfinApi.userId!!, itemId, true)
        }
    }

    override suspend fun markAsFavorite(itemId: UUID) {
        withContext(Dispatchers.IO) {
            database.setFavorite(jellyfinApi.userId!!, itemId, true)
            database.setUserDataToBeSynced(jellyfinApi.userId!!, itemId, true)
        }
    }

    override suspend fun unmarkAsFavorite(itemId: UUID) {
        withContext(Dispatchers.IO) {
            database.setFavorite(jellyfinApi.userId!!, itemId, false)
            database.setUserDataToBeSynced(jellyfinApi.userId!!, itemId, true)
        }
    }

    override suspend fun markAsPlayed(itemId: UUID) {
        withContext(Dispatchers.IO) {
            database.setPlayed(jellyfinApi.userId!!, itemId, true)
            database.setPlaybackPositionTicks(itemId, jellyfinApi.userId!!, 0)
            database.setUserDataToBeSynced(jellyfinApi.userId!!, itemId, true)
        }
    }

    override suspend fun markAsUnplayed(itemId: UUID) {
        withContext(Dispatchers.IO) {
            database.setPlayed(jellyfinApi.userId!!, itemId, false)
            database.setUserDataToBeSynced(jellyfinApi.userId!!, itemId, true)
        }
    }

    override fun getBaseUrl(): String {
        return ""
    }

    override suspend fun updateDeviceName(name: String) {
        // No-op in offline mode
    }

    override suspend fun getUserConfiguration(): UserConfiguration? {
        return null
    }

    override suspend fun getDownloads(): List<JellyCastItem> =
        withContext(Dispatchers.IO) {
            val serverId = appPreferences.getValue(appPreferences.currentServer)
            val userId = jellyfinApi.userId ?: return@withContext emptyList()
            val baseUrl = getBaseUrl()
            val items = mutableListOf<JellyCastItem>()
            timber.log.Timber.tag("Repo").d("getDownloads(offline) serverId=%s userId=%s baseUrl=%s", serverId, userId, baseUrl)
            items.addAll(
                database.getMovies()
                    .filter { dto -> serverId == null || dto.serverId == serverId || dto.serverId == null }
                    .map { movieDto -> movieDto.toJellyCastMovie(database, userId, baseUrl) },
            )
            items.addAll(
                database.getShows()
                    .filter { dto -> serverId == null || dto.serverId == serverId || dto.serverId == null }
                    .map { showDto -> showDto.toJellyCastShow(database, userId, baseUrl) },
            )
            items.addAll(
                database.getEpisodes()
                    .filter { dto -> serverId == null || dto.serverId == serverId || dto.serverId == null }
                    .map { episodeDto -> episodeDto.toJellyCastEpisode(database, userId, baseUrl) },
            )
            timber.log.Timber.tag("Repo").d(
                "getDownloads(offline) -> total=%d movies=%d shows=%d episodes=%d",
                items.size,
                items.count { it is dev.jdtech.jellyfin.models.JellyCastMovie },
                items.count { it is dev.jdtech.jellyfin.models.JellyCastShow },
                items.count { it is dev.jdtech.jellyfin.models.JellyCastEpisode },
            )
            items
        }

    override fun getUserId(): UUID {
        return jellyfinApi.userId!!
    }
}
