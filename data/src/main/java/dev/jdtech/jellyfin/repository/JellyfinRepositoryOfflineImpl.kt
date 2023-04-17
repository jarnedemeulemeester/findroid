package dev.jdtech.jellyfin.repository

import android.content.Context
import android.os.Environment
import androidx.paging.PagingData
import dev.jdtech.jellyfin.AppPreferences
import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.models.FindroidCollection
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.FindroidMovie
import dev.jdtech.jellyfin.models.FindroidSeason
import dev.jdtech.jellyfin.models.FindroidShow
import dev.jdtech.jellyfin.models.FindroidSource
import dev.jdtech.jellyfin.models.Intro
import dev.jdtech.jellyfin.models.SortBy
import dev.jdtech.jellyfin.models.TrickPlayManifest
import dev.jdtech.jellyfin.models.toFindroidEpisode
import dev.jdtech.jellyfin.models.toFindroidMovie
import dev.jdtech.jellyfin.models.toFindroidSeason
import dev.jdtech.jellyfin.models.toFindroidShow
import dev.jdtech.jellyfin.models.toFindroidSource
import dev.jdtech.jellyfin.models.toIntro
import dev.jdtech.jellyfin.models.toTrickPlayManifest
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemFields
import org.jellyfin.sdk.model.api.SortOrder
import org.jellyfin.sdk.model.api.UserConfiguration

class JellyfinRepositoryOfflineImpl(
    private val context: Context,
    private val jellyfinApi: JellyfinApi,
    private val database: ServerDatabaseDao,
    private val appPreferences: AppPreferences,
) : JellyfinRepository {
    override suspend fun getUserViews(): List<BaseItemDto> {
        return emptyList()
    }

    override suspend fun getItem(itemId: UUID): BaseItemDto {
        TODO("Not yet implemented")
    }

    override suspend fun getMovie(itemId: UUID): FindroidMovie =
        withContext(Dispatchers.IO) {
            database.getMovie(itemId).toFindroidMovie(database, jellyfinApi.userId!!)
        }

    override suspend fun getShow(itemId: UUID): FindroidShow =
        withContext(Dispatchers.IO) {
            database.getShow(itemId).toFindroidShow(database, jellyfinApi.userId!!)
        }

    override suspend fun getSeason(itemId: UUID): FindroidSeason =
        withContext(Dispatchers.IO) {
            database.getSeason(itemId).toFindroidSeason(database, jellyfinApi.userId!!)
        }

    override suspend fun getEpisode(itemId: UUID): FindroidEpisode =
        withContext(Dispatchers.IO) {
            database.getEpisode(itemId).toFindroidEpisode(database, jellyfinApi.userId!!)
        }

    override suspend fun getLibraries(): List<FindroidCollection> {
        TODO("Not yet implemented")
    }

    override suspend fun getItems(
        parentId: UUID?,
        includeTypes: List<BaseItemKind>?,
        recursive: Boolean,
        sortBy: SortBy,
        sortOrder: SortOrder,
        startIndex: Int?,
        limit: Int?
    ): List<FindroidItem> {
        TODO("Not yet implemented")
    }

    override suspend fun getItemsPaging(
        parentId: UUID?,
        includeTypes: List<BaseItemKind>?,
        recursive: Boolean,
        sortBy: SortBy,
        sortOrder: SortOrder
    ): Flow<PagingData<FindroidItem>> {
        TODO("Not yet implemented")
    }

    override suspend fun getPersonItems(
        personIds: List<UUID>,
        includeTypes: List<BaseItemKind>?,
        recursive: Boolean
    ): List<FindroidItem> {
        TODO("Not yet implemented")
    }

    override suspend fun getFavoriteItems(): List<FindroidItem> {
        TODO("Not yet implemented")
    }

    override suspend fun getSearchItems(searchQuery: String): List<FindroidItem> {
        TODO("Not yet implemented")
    }

    override suspend fun getResumeItems(): List<FindroidItem> {
        val items = database.getResumeItems(appPreferences.currentServer!!)
        return items.map {
            it.toFindroidMovie(database, jellyfinApi.userId!!)
        }
    }

    override suspend fun getLatestMedia(parentId: UUID): List<FindroidItem> {
        return emptyList()
    }

    override suspend fun getSeasons(seriesId: UUID): List<FindroidSeason> =
        withContext(Dispatchers.IO) {
            database.getSeasonsByShowId(seriesId).map { it.toFindroidSeason(database, jellyfinApi.userId!!) }
        }

    override suspend fun getNextUp(seriesId: UUID?): List<FindroidEpisode> {
        return emptyList()
    }

    override suspend fun getEpisodes(
        seriesId: UUID,
        seasonId: UUID,
        fields: List<ItemFields>?,
        startItemId: UUID?,
        limit: Int?
    ): List<FindroidEpisode> =
        withContext(Dispatchers.IO) {
            database.getEpisodesBySeasonId(seasonId).map { it.toFindroidEpisode(database, jellyfinApi.userId!!) }
        }

    override suspend fun getMediaSources(itemId: UUID): List<FindroidSource> =
        withContext(Dispatchers.IO) {
            database.getSources(itemId).map { it.toFindroidSource(database) }
        }

    override suspend fun getStreamUrl(itemId: UUID, mediaSourceId: String): String {
        TODO("Not yet implemented")
    }

    override suspend fun getIntroTimestamps(itemId: UUID): Intro? =
        withContext(Dispatchers.IO) {
            database.getIntro(itemId)?.toIntro()
        }

    override suspend fun getTrickPlayManifest(itemId: UUID): TrickPlayManifest? =
        withContext(Dispatchers.IO) {
            database.getTrickPlayManifest(itemId)?.toTrickPlayManifest()
        }

    override suspend fun getTrickPlayData(itemId: UUID, width: Int): ByteArray? =
        withContext(Dispatchers.IO) {
            val trickPlayManifest = database.getTrickPlayManifest(itemId)
            if (trickPlayManifest != null) {
                return@withContext File(
                    context.getExternalFilesDir(Environment.DIRECTORY_MOVIES),
                    "$itemId.bif"
                ).readBytes()
            }
            null
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
        isPaused: Boolean
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
        TODO("Not yet implemented")
    }

    override suspend fun getUserConfiguration(): UserConfiguration? {
        return null
    }

    override suspend fun getDownloads(currentServer: Boolean): List<FindroidItem> =
        withContext(Dispatchers.IO) {
            val items = mutableListOf<FindroidItem>()
            when (currentServer) {
                true -> {
                    items.addAll(
                        database.getMoviesByServerId(appPreferences.currentServer!!)
                            .map { it.toFindroidMovie(database, jellyfinApi.userId!!) }
                    )
                    items.addAll(
                        database.getShowsByServerId(appPreferences.currentServer!!)
                            .map { it.toFindroidShow(database, jellyfinApi.userId!!) }
                    )
                    items
                }
                false -> {
                    items.addAll(
                        database.getMovies().map { it.toFindroidMovie(database, jellyfinApi.userId!!) }
                    )
                    items.addAll(
                        database.getShows().map { it.toFindroidShow(database, jellyfinApi.userId!!) }
                    )
                    items
                }
            }
        }

    override fun getUserId(): UUID {
        return jellyfinApi.userId!!
    }
}
