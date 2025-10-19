package dev.jdtech.jellyfin.repository

import android.content.Context
import androidx.paging.Pager
import androidx.paging.PagingConfig
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
import dev.jdtech.jellyfin.models.toJellyCastCollection
import dev.jdtech.jellyfin.models.toJellyCastEpisode
import dev.jdtech.jellyfin.models.toJellyCastItem
import dev.jdtech.jellyfin.models.toJellyCastMovie
import dev.jdtech.jellyfin.models.toJellyCastPerson
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
import org.jellyfin.sdk.model.api.DeviceOptionsDto
import org.jellyfin.sdk.model.api.DeviceProfile
import org.jellyfin.sdk.model.api.GeneralCommandType
import org.jellyfin.sdk.model.api.ItemFields
import org.jellyfin.sdk.model.api.ItemFilter
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.MediaType
import org.jellyfin.sdk.model.api.PlaybackInfoDto
import org.jellyfin.sdk.model.api.PublicSystemInfo
import org.jellyfin.sdk.model.api.SortOrder
import org.jellyfin.sdk.model.api.SubtitleDeliveryMethod
import org.jellyfin.sdk.model.api.SubtitleProfile
import org.jellyfin.sdk.model.api.UserConfiguration
import timber.log.Timber
import java.io.File
import java.util.UUID

class JellyfinRepositoryImpl(
    private val context: Context,
    private val jellyfinApi: JellyfinApi,
    private val database: ServerDatabaseDao,
    private val appPreferences: AppPreferences,
) : JellyfinRepository {
    override suspend fun getPublicSystemInfo(): PublicSystemInfo = withContext(Dispatchers.IO) {
        jellyfinApi.systemApi.getPublicSystemInfo().content
    }

    override suspend fun getUserViews(): List<BaseItemDto> = withContext(Dispatchers.IO) {
        jellyfinApi.viewsApi.getUserViews(jellyfinApi.userId!!).content.items
    }

    override suspend fun getEpisode(itemId: UUID): JellyCastEpisode =
        withContext(Dispatchers.IO) {
            jellyfinApi.userLibraryApi.getItem(
                itemId,
                jellyfinApi.userId!!,
            ).content.toJellyCastEpisode(this@JellyfinRepositoryImpl, database)!!
        }

    override suspend fun getMovie(itemId: UUID): JellyCastMovie =
        withContext(Dispatchers.IO) {
            jellyfinApi.userLibraryApi.getItem(
                itemId,
                jellyfinApi.userId!!,
            ).content.toJellyCastMovie(this@JellyfinRepositoryImpl, database)
        }

    override suspend fun getShow(itemId: UUID): JellyCastShow =
        withContext(Dispatchers.IO) {
            jellyfinApi.userLibraryApi.getItem(
                itemId,
                jellyfinApi.userId!!,
            ).content.toJellyCastShow(this@JellyfinRepositoryImpl)
        }

    override suspend fun getSeason(itemId: UUID): JellyCastSeason =
        withContext(Dispatchers.IO) {
            jellyfinApi.userLibraryApi.getItem(
                itemId,
                jellyfinApi.userId!!,
            ).content.toJellyCastSeason(this@JellyfinRepositoryImpl)
        }

    override suspend fun getLibraries(): List<JellyCastCollection> =
        withContext(Dispatchers.IO) {
            jellyfinApi.itemsApi.getItems(
                jellyfinApi.userId!!,
            ).content.items
                .mapNotNull { it.toJellyCastCollection(this@JellyfinRepositoryImpl) }
        }

    override suspend fun getItems(
        parentId: UUID?,
        includeTypes: List<BaseItemKind>?,
        recursive: Boolean,
        sortBy: SortBy,
        sortOrder: SortOrder,
        startIndex: Int?,
        limit: Int?,
    ): List<JellyCastItem> =
        withContext(Dispatchers.IO) {
            jellyfinApi.itemsApi.getItems(
                jellyfinApi.userId!!,
                parentId = parentId,
                includeItemTypes = includeTypes,
                recursive = recursive,
                fields = listOf(ItemFields.GENRES),
                sortBy = listOf(ItemSortBy.fromName(sortBy.sortString)),
                sortOrder = listOf(sortOrder),
                startIndex = startIndex,
                limit = limit,
            ).content.items
                .mapNotNull { it.toJellyCastItem(this@JellyfinRepositoryImpl, database) }
        }

    override suspend fun getItemsPaging(
        parentId: UUID?,
        includeTypes: List<BaseItemKind>?,
        recursive: Boolean,
        sortBy: SortBy,
        sortOrder: SortOrder,
    ): Flow<PagingData<JellyCastItem>> {
        return Pager(
            config = PagingConfig(
                pageSize = 10,
                enablePlaceholders = false,
            ),
            pagingSourceFactory = {
                ItemsPagingSource(
                    this,
                    parentId,
                    includeTypes,
                    recursive,
                    sortBy,
                    sortOrder,
                )
            },
        ).flow
    }

    override suspend fun getPerson(personId: UUID): JellyCastPerson =
        withContext(Dispatchers.IO) {
            jellyfinApi.userLibraryApi.getItem(personId, jellyfinApi.userId!!).content.toJellyCastPerson(this@JellyfinRepositoryImpl)
        }

    override suspend fun getPersonItems(
        personIds: List<UUID>,
        includeTypes: List<BaseItemKind>?,
        recursive: Boolean,
    ): List<JellyCastItem> = withContext(Dispatchers.IO) {
        jellyfinApi.itemsApi.getItems(
            jellyfinApi.userId!!,
            personIds = personIds,
            includeItemTypes = includeTypes,
            recursive = recursive,
        ).content.items
            .mapNotNull {
                it.toJellyCastItem(this@JellyfinRepositoryImpl, database)
            }
    }

    override suspend fun getFavoriteItems(): List<JellyCastItem> =
        withContext(Dispatchers.IO) {
            jellyfinApi.itemsApi.getItems(
                jellyfinApi.userId!!,
                filters = listOf(ItemFilter.IS_FAVORITE),
                includeItemTypes = listOf(
                    BaseItemKind.MOVIE,
                    BaseItemKind.SERIES,
                    BaseItemKind.EPISODE,
                ),
                recursive = true,
            ).content.items
                .mapNotNull { it.toJellyCastItem(this@JellyfinRepositoryImpl, database) }
        }

    override suspend fun getSearchItems(query: String): List<JellyCastItem> =
        withContext(Dispatchers.IO) {
            jellyfinApi.itemsApi.getItems(
                jellyfinApi.userId!!,
                searchTerm = query,
                includeItemTypes = listOf(
                    BaseItemKind.MOVIE,
                    BaseItemKind.SERIES,
                ),
                recursive = true,
            ).content.items
                .mapNotNull { it.toJellyCastItem(this@JellyfinRepositoryImpl, database) }
        }

    override suspend fun getSuggestions(): List<JellyCastItem> {
        val items = withContext(Dispatchers.IO) {
            jellyfinApi.suggestionsApi.getSuggestions(
                jellyfinApi.userId!!,
                limit = 6,
                type = listOf(BaseItemKind.MOVIE, BaseItemKind.SERIES),
            ).content.items
        }
        return items.mapNotNull {
            it.toJellyCastItem(this, database)
        }
    }

    override suspend fun getResumeItems(): List<JellyCastItem> {
        val items = withContext(Dispatchers.IO) {
            jellyfinApi.itemsApi.getResumeItems(
                jellyfinApi.userId!!,
                limit = 12,
                includeItemTypes = listOf(BaseItemKind.MOVIE, BaseItemKind.EPISODE),
            ).content.items
        }
        return items.mapNotNull {
            it.toJellyCastItem(this, database)
        }
    }

    override suspend fun getLatestMedia(parentId: UUID): List<JellyCastItem> {
        val items = withContext(Dispatchers.IO) {
            jellyfinApi.userLibraryApi.getLatestMedia(
                jellyfinApi.userId!!,
                parentId = parentId,
                limit = 16,
            ).content
        }
        return items.mapNotNull {
            it.toJellyCastItem(this, database)
        }
    }

    override suspend fun getSeasons(seriesId: UUID, offline: Boolean): List<JellyCastSeason> =
        withContext(Dispatchers.IO) {
            if (!offline) {
                jellyfinApi.showsApi.getSeasons(seriesId, jellyfinApi.userId!!).content.items
                    .map { it.toJellyCastSeason(this@JellyfinRepositoryImpl) }
            } else {
                database.getSeasonsByShowId(seriesId).map { it.toJellyCastSeason(database, jellyfinApi.userId!!) }
            }
        }

    override suspend fun getNextUp(seriesId: UUID?): List<JellyCastEpisode> =
        withContext(Dispatchers.IO) {
            jellyfinApi.showsApi.getNextUp(
                jellyfinApi.userId!!,
                limit = 24,
                seriesId = seriesId,
                enableResumable = false,
            ).content.items
                .mapNotNull { it.toJellyCastEpisode(this@JellyfinRepositoryImpl) }
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
            if (!offline) {
                jellyfinApi.showsApi.getEpisodes(
                    seriesId,
                    jellyfinApi.userId!!,
                    seasonId = seasonId,
                    fields = fields,
                    startItemId = startItemId,
                    limit = limit,
                ).content.items
                    .mapNotNull { it.toJellyCastEpisode(this@JellyfinRepositoryImpl, database) }
            } else {
                database.getEpisodesBySeasonId(seasonId).map { it.toJellyCastEpisode(database, jellyfinApi.userId!!) }
            }
        }

    override suspend fun getMediaSources(itemId: UUID, includePath: Boolean): List<JellyCastSource> =
        withContext(Dispatchers.IO) {
            val sources = mutableListOf<JellyCastSource>()
            sources.addAll(
                jellyfinApi.mediaInfoApi.getPostedPlaybackInfo(
                    itemId,
                    PlaybackInfoDto(
                        userId = jellyfinApi.userId!!,
                        deviceProfile = DeviceProfile(
                            name = "Direct play all",
                            maxStaticBitrate = 1_000_000_000,
                            maxStreamingBitrate = 1_000_000_000,
                            codecProfiles = emptyList(),
                            containerProfiles = emptyList(),
                            directPlayProfiles = emptyList(),
                            transcodingProfiles = emptyList(),
                            subtitleProfiles = listOf(
                                SubtitleProfile("srt", SubtitleDeliveryMethod.EXTERNAL),
                                SubtitleProfile("ass", SubtitleDeliveryMethod.EXTERNAL),
                            ),
                        ),
                        maxStreamingBitrate = 1_000_000_000,
                    ),
                ).content.mediaSources.map {
                    it.toJellyCastSource(
                        this@JellyfinRepositoryImpl,
                        itemId,
                        includePath,
                    )
                },
            )
            sources.addAll(
                database.getSources(itemId).map { it.toJellyCastSource(database) },
            )
            sources
        }

    override suspend fun getStreamUrl(itemId: UUID, mediaSourceId: String): String =
        withContext(Dispatchers.IO) {
            try {
                jellyfinApi.videosApi.getVideoStreamUrl(
                    itemId,
                    static = true,
                    mediaSourceId = mediaSourceId,
                )
            } catch (e: Exception) {
                Timber.e(e)
                ""
            }
        }

    override suspend fun getSegments(itemId: UUID): List<JellyCastSegment> =
        withContext(Dispatchers.IO) {
            val databaseSegments = database.getSegments(itemId).map {
                it.toJellyCastSegment()
            }

            if (databaseSegments.isNotEmpty()) {
                return@withContext databaseSegments
            }

            try {
                val apiSegments = jellyfinApi.mediaSegmentsApi.getItemSegments(itemId).content.items.map {
                    it.toJellyCastSegment()
                }

                return@withContext apiSegments
            } catch (e: Exception) {
                Timber.e(e)
                return@withContext emptyList()
            }
        }

    override suspend fun getTrickplayData(itemId: UUID, width: Int, index: Int): ByteArray? =
        withContext(Dispatchers.IO) {
            try {
                try {
                    val sources = File(context.filesDir, "trickplay/$itemId").listFiles()
                    if (sources != null) {
                        return@withContext File(sources.first(), index.toString()).readBytes()
                    }
                } catch (_: Exception) { }

                return@withContext jellyfinApi.trickplayApi.getTrickplayTileImage(itemId, width, index).content
            } catch (_: Exception) {
                return@withContext null
            }
        }

    override suspend fun postCapabilities() {
        Timber.d("Sending capabilities")
        withContext(Dispatchers.IO) {
            jellyfinApi.sessionApi.postCapabilities(
                playableMediaTypes = listOf(MediaType.VIDEO),
                supportedCommands = listOf(
                    GeneralCommandType.VOLUME_UP,
                    GeneralCommandType.VOLUME_DOWN,
                    GeneralCommandType.TOGGLE_MUTE,
                    GeneralCommandType.SET_AUDIO_STREAM_INDEX,
                    GeneralCommandType.SET_SUBTITLE_STREAM_INDEX,
                    GeneralCommandType.MUTE,
                    GeneralCommandType.UNMUTE,
                    GeneralCommandType.SET_VOLUME,
                    GeneralCommandType.DISPLAY_MESSAGE,
                    GeneralCommandType.PLAY,
                    GeneralCommandType.PLAY_STATE,
                    GeneralCommandType.PLAY_NEXT,
                    GeneralCommandType.PLAY_MEDIA_SOURCE,
                ),
                supportsMediaControl = true,
            )
        }
    }

    override suspend fun postPlaybackStart(itemId: UUID) {
        Timber.d("Sending start $itemId")
        withContext(Dispatchers.IO) {
            jellyfinApi.playStateApi.onPlaybackStart(itemId)
        }
    }

    override suspend fun postPlaybackStop(
        itemId: UUID,
        positionTicks: Long,
        playedPercentage: Int,
    ) {
        Timber.d("Sending stop $itemId")
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
            try {
                jellyfinApi.playStateApi.onPlaybackStopped(
                    itemId,
                    positionTicks = positionTicks,
                )
            } catch (_: Exception) {
                database.setUserDataToBeSynced(jellyfinApi.userId!!, itemId, true)
            }
        }
    }

    override suspend fun postPlaybackProgress(
        itemId: UUID,
        positionTicks: Long,
        isPaused: Boolean,
    ) {
        Timber.d("Posting progress of $itemId, position: $positionTicks")
        withContext(Dispatchers.IO) {
            database.setPlaybackPositionTicks(itemId, jellyfinApi.userId!!, positionTicks)
            try {
                jellyfinApi.playStateApi.onPlaybackProgress(
                    itemId,
                    positionTicks = positionTicks,
                    isPaused = isPaused,
                )
            } catch (_: Exception) {
                database.setUserDataToBeSynced(jellyfinApi.userId!!, itemId, true)
            }
        }
    }

    override suspend fun markAsFavorite(itemId: UUID) {
        withContext(Dispatchers.IO) {
            database.setFavorite(jellyfinApi.userId!!, itemId, true)
            try {
                jellyfinApi.userLibraryApi.markFavoriteItem(itemId)
            } catch (_: Exception) {
                database.setUserDataToBeSynced(jellyfinApi.userId!!, itemId, true)
            }
        }
    }

    override suspend fun unmarkAsFavorite(itemId: UUID) {
        withContext(Dispatchers.IO) {
            database.setFavorite(jellyfinApi.userId!!, itemId, false)
            try {
                jellyfinApi.userLibraryApi.unmarkFavoriteItem(itemId)
            } catch (_: Exception) {
                database.setUserDataToBeSynced(jellyfinApi.userId!!, itemId, true)
            }
        }
    }

    override suspend fun markAsPlayed(itemId: UUID) {
        withContext(Dispatchers.IO) {
            database.setPlayed(jellyfinApi.userId!!, itemId, true)
            try {
                jellyfinApi.playStateApi.markPlayedItem(itemId)
            } catch (_: Exception) {
                database.setUserDataToBeSynced(jellyfinApi.userId!!, itemId, true)
            }
        }
    }

    override suspend fun markAsUnplayed(itemId: UUID) {
        withContext(Dispatchers.IO) {
            database.setPlayed(jellyfinApi.userId!!, itemId, false)
            try {
                jellyfinApi.playStateApi.markUnplayedItem(itemId)
            } catch (_: Exception) {
                database.setUserDataToBeSynced(jellyfinApi.userId!!, itemId, true)
            }
        }
    }

    override fun getBaseUrl() = jellyfinApi.api.baseUrl.orEmpty()

    override suspend fun updateDeviceName(name: String) {
        jellyfinApi.jellyfin.deviceInfo?.id?.let { id ->
            withContext(Dispatchers.IO) {
                jellyfinApi.devicesApi.updateDeviceOptions(
                    id,
                    DeviceOptionsDto(0, customName = name),
                )
            }
        }
    }

    override suspend fun getUserConfiguration(): UserConfiguration = withContext(Dispatchers.IO) {
        jellyfinApi.userApi.getCurrentUser().content.configuration!!
    }

    override suspend fun getDownloads(): List<JellyCastItem> =
        withContext(Dispatchers.IO) {
            val serverId = appPreferences.getValue(appPreferences.currentServer)
            val userId = jellyfinApi.userId ?: return@withContext emptyList()
            val baseUrl = getBaseUrl()
            val items = mutableListOf<JellyCastItem>()
            Timber.tag("Repo").d("getDownloads(online) serverId=%s userId=%s baseUrl=%s", serverId, userId, baseUrl)

            // Include entries for current server and any entries where serverId is null (created before we set serverId)
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
            Timber.tag("Repo").d(
                "getDownloads(online) -> total=%d movies=%d shows=%d episodes=%d",
                items.size,
                items.count { it is JellyCastMovie },
                items.count { it is JellyCastShow },
                items.count { it is JellyCastEpisode },
            )
            items
        }

    override fun getUserId(): UUID {
        return jellyfinApi.userId!!
    }
}
