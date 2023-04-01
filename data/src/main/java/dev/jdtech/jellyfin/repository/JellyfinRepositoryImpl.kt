package dev.jdtech.jellyfin.repository

import android.content.Context
import android.os.Environment
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import dev.jdtech.jellyfin.AppPreferences
import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.models.FindroidCollection
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidIntro
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.FindroidMovie
import dev.jdtech.jellyfin.models.FindroidSeason
import dev.jdtech.jellyfin.models.FindroidShow
import dev.jdtech.jellyfin.models.FindroidSource
import dev.jdtech.jellyfin.models.Intro
import dev.jdtech.jellyfin.models.SortBy
import dev.jdtech.jellyfin.models.TrickPlayManifest
import dev.jdtech.jellyfin.models.toFindroidEpisode
import dev.jdtech.jellyfin.models.toFindroidIntro
import dev.jdtech.jellyfin.models.toFindroidItem
import dev.jdtech.jellyfin.models.toFindroidMovie
import dev.jdtech.jellyfin.models.toFindroidSeason
import dev.jdtech.jellyfin.models.toFindroidShow
import dev.jdtech.jellyfin.models.toFindroidSource
import dev.jdtech.jellyfin.models.toIntro
import dev.jdtech.jellyfin.models.toJellyfinCollection
import dev.jdtech.jellyfin.models.toTrickPlayManifest
import io.ktor.util.cio.toByteArray
import io.ktor.utils.io.ByteReadChannel
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.api.client.extensions.get
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.DeviceOptionsDto
import org.jellyfin.sdk.model.api.DeviceProfile
import org.jellyfin.sdk.model.api.DirectPlayProfile
import org.jellyfin.sdk.model.api.DlnaProfileType
import org.jellyfin.sdk.model.api.GeneralCommandType
import org.jellyfin.sdk.model.api.ItemFields
import org.jellyfin.sdk.model.api.ItemFilter
import org.jellyfin.sdk.model.api.PlaybackInfoDto
import org.jellyfin.sdk.model.api.SortOrder
import org.jellyfin.sdk.model.api.SubtitleDeliveryMethod
import org.jellyfin.sdk.model.api.SubtitleProfile
import org.jellyfin.sdk.model.api.UserConfiguration
import timber.log.Timber

class JellyfinRepositoryImpl(
    private val context: Context,
    private val jellyfinApi: JellyfinApi,
    private val database: ServerDatabaseDao,
    private val appPreferences: AppPreferences,
) : JellyfinRepository {
    override suspend fun getUserViews(): List<BaseItemDto> = withContext(Dispatchers.IO) {
        jellyfinApi.viewsApi.getUserViews(jellyfinApi.userId!!).content.items.orEmpty()
    }

    override suspend fun getItem(itemId: UUID): BaseItemDto = withContext(Dispatchers.IO) {
        jellyfinApi.userLibraryApi.getItem(jellyfinApi.userId!!, itemId).content
    }

    override suspend fun getEpisode(itemId: UUID): FindroidEpisode =
        withContext(Dispatchers.IO) {
            jellyfinApi.userLibraryApi.getItem(
                jellyfinApi.userId!!,
                itemId
            ).content.toFindroidEpisode(this@JellyfinRepositoryImpl, database)
        }

    override suspend fun getMovie(itemId: UUID): FindroidMovie =
        withContext(Dispatchers.IO) {
            jellyfinApi.userLibraryApi.getItem(
                jellyfinApi.userId!!,
                itemId
            ).content.toFindroidMovie(this@JellyfinRepositoryImpl, database)
        }

    override suspend fun getShow(itemId: UUID): FindroidShow =
        withContext(Dispatchers.IO) {
            jellyfinApi.userLibraryApi.getItem(
                jellyfinApi.userId!!,
                itemId
            ).content.toFindroidShow()
        }

    override suspend fun getSeason(itemId: UUID): FindroidSeason =
        withContext(Dispatchers.IO) {
            jellyfinApi.userLibraryApi.getItem(
                jellyfinApi.userId!!,
                itemId
            ).content.toFindroidSeason()
        }

    override suspend fun getLibraries(): List<FindroidCollection> =
        withContext(Dispatchers.IO) {
            jellyfinApi.itemsApi.getItems(
                jellyfinApi.userId!!,
            ).content.items
                .orEmpty()
                .map { it.toJellyfinCollection() }
        }

    override suspend fun getItems(
        parentId: UUID?,
        includeTypes: List<BaseItemKind>?,
        recursive: Boolean,
        sortBy: SortBy,
        sortOrder: SortOrder,
        startIndex: Int?,
        limit: Int?
    ): List<FindroidItem> =
        withContext(Dispatchers.IO) {
            jellyfinApi.itemsApi.getItems(
                jellyfinApi.userId!!,
                parentId = parentId,
                includeItemTypes = includeTypes,
                recursive = recursive,
                sortBy = listOf(sortBy.SortString),
                sortOrder = listOf(sortOrder),
                startIndex = startIndex,
                limit = limit,
            ).content.items
                .orEmpty()
                .mapNotNull { it.toFindroidItem(this@JellyfinRepositoryImpl, database) }
        }

    override suspend fun getItemsPaging(
        parentId: UUID?,
        includeTypes: List<BaseItemKind>?,
        recursive: Boolean,
        sortBy: SortBy,
        sortOrder: SortOrder
    ): Flow<PagingData<FindroidItem>> {
        return Pager(
            config = PagingConfig(
                pageSize = 10,
                maxSize = 100,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                ItemsPagingSource(
                    this,
                    parentId,
                    includeTypes,
                    recursive,
                    sortBy,
                    sortOrder
                )
            }
        ).flow
    }

    override suspend fun getPersonItems(
        personIds: List<UUID>,
        includeTypes: List<BaseItemKind>?,
        recursive: Boolean
    ): List<FindroidItem> = withContext(Dispatchers.IO) {
        jellyfinApi.itemsApi.getItems(
            jellyfinApi.userId!!,
            personIds = personIds,
            includeItemTypes = includeTypes,
            recursive = recursive
        ).content.items
            .orEmpty()
            .mapNotNull {
                it.toFindroidItem(this@JellyfinRepositoryImpl, database)
            }
    }

    override suspend fun getFavoriteItems(): List<FindroidItem> =
        withContext(Dispatchers.IO) {
            jellyfinApi.itemsApi.getItems(
                jellyfinApi.userId!!,
                filters = listOf(ItemFilter.IS_FAVORITE),
                includeItemTypes = listOf(
                    BaseItemKind.MOVIE,
                    BaseItemKind.SERIES,
                    BaseItemKind.EPISODE
                ),
                recursive = true
            ).content.items
                .orEmpty()
                .mapNotNull { it.toFindroidItem(this@JellyfinRepositoryImpl, database) }
        }

    override suspend fun getSearchItems(searchQuery: String): List<FindroidItem> =
        withContext(Dispatchers.IO) {
            jellyfinApi.itemsApi.getItems(
                jellyfinApi.userId!!,
                searchTerm = searchQuery,
                includeItemTypes = listOf(
                    BaseItemKind.MOVIE,
                    BaseItemKind.SERIES,
                    BaseItemKind.EPISODE
                ),
                recursive = true
            ).content.items
                .orEmpty()
                .mapNotNull { it.toFindroidItem(this@JellyfinRepositoryImpl, database) }
        }

    override suspend fun getResumeItems(): List<FindroidItem> {
        val items = withContext(Dispatchers.IO) {
            jellyfinApi.itemsApi.getResumeItems(
                jellyfinApi.userId!!,
                includeItemTypes = listOf(BaseItemKind.MOVIE, BaseItemKind.EPISODE),
            ).content.items.orEmpty()
        }
        return items.mapNotNull {
            it.toFindroidItem(this, database)
        }
    }

    override suspend fun getLatestMedia(parentId: UUID): List<FindroidItem> {
        val items = withContext(Dispatchers.IO) {
            jellyfinApi.userLibraryApi.getLatestMedia(
                jellyfinApi.userId!!,
                parentId = parentId
            ).content
        }
        return items.mapNotNull {
            it.toFindroidItem(this, database)
        }
    }

    override suspend fun getSeasons(seriesId: UUID): List<FindroidSeason> =
        withContext(Dispatchers.IO) {
            jellyfinApi.showsApi.getSeasons(seriesId, jellyfinApi.userId!!).content.items
                .orEmpty()
                .map { it.toFindroidSeason() }
        }

    override suspend fun getNextUp(seriesId: UUID?): List<FindroidEpisode> =
        withContext(Dispatchers.IO) {
            jellyfinApi.showsApi.getNextUp(
                jellyfinApi.userId!!,
                seriesId = seriesId?.toString(),
            ).content.items
                .orEmpty()
                .map { it.toFindroidEpisode(this@JellyfinRepositoryImpl) }
        }

    override suspend fun getEpisodes(
        seriesId: UUID,
        seasonId: UUID,
        fields: List<ItemFields>?,
        startItemId: UUID?,
        limit: Int?,
    ): List<FindroidEpisode> =
        withContext(Dispatchers.IO) {
            jellyfinApi.showsApi.getEpisodes(
                seriesId,
                jellyfinApi.userId!!,
                seasonId = seasonId,
                fields = fields,
                startItemId = startItemId,
                limit = limit,
            ).content.items
                .orEmpty()
                .map { it.toFindroidEpisode(this@JellyfinRepositoryImpl, database) }
        }

    override suspend fun getMediaSources(itemId: UUID): List<FindroidSource> =
        withContext(Dispatchers.IO) {
            val sources = mutableListOf<FindroidSource>()
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
                            directPlayProfiles = listOf(
                                DirectPlayProfile(type = DlnaProfileType.VIDEO),
                                DirectPlayProfile(type = DlnaProfileType.AUDIO)
                            ),
                            transcodingProfiles = emptyList(),
                            responseProfiles = emptyList(),
                            subtitleProfiles = listOf(
                                SubtitleProfile("srt", SubtitleDeliveryMethod.EXTERNAL),
                                SubtitleProfile("vtt", SubtitleDeliveryMethod.EXTERNAL),
                                SubtitleProfile("ass", SubtitleDeliveryMethod.EXTERNAL),
                            ),
                            xmlRootAttributes = emptyList(),
                            supportedMediaTypes = "",
                            enableAlbumArtInDidl = false,
                            enableMsMediaReceiverRegistrar = false,
                            enableSingleAlbumArtLimit = false,
                            enableSingleSubtitleLimit = false,
                            ignoreTranscodeByteRangeRequests = false,
                            maxAlbumArtHeight = 1_000_000_000,
                            maxAlbumArtWidth = 1_000_000_000,
                            requiresPlainFolders = false,
                            requiresPlainVideoItems = false,
                            timelineOffsetSeconds = 0
                        ),
                        maxStreamingBitrate = 1_000_000_000,
                    )
                ).content.mediaSources.map {
                    it.toFindroidSource(
                        this@JellyfinRepositoryImpl,
                        itemId
                    )
                }
            )
            sources.addAll(
                database.getSources(itemId).map { it.toFindroidSource(database) }
            )
            sources
        }

    override suspend fun getStreamUrl(itemId: UUID, mediaSourceId: String): String =
        withContext(Dispatchers.IO) {
            try {
                jellyfinApi.videosApi.getVideoStreamUrl(
                    itemId,
                    static = true,
                    mediaSourceId = mediaSourceId
                )
            } catch (e: Exception) {
                Timber.e(e)
                ""
            }
        }

    override suspend fun getIntroTimestamps(itemId: UUID): Intro? =
        withContext(Dispatchers.IO) {
            val intro = database.getIntro(itemId)?.toIntro()

            if (intro != null) {
                return@withContext intro
            }

            // https://github.com/ConfusedPolarBear/intro-skipper/blob/master/docs/api.md
            val pathParameters = mutableMapOf<String, UUID>()
            pathParameters["itemId"] = itemId

            try {
                return@withContext jellyfinApi.api.get<Intro>(
                    "/Episode/{itemId}/IntroTimestamps/v1",
                    pathParameters
                ).content
            } catch (e: Exception) {
                return@withContext null
            }
        }

    override suspend fun getTrickPlayManifest(itemId: UUID): TrickPlayManifest? =
        withContext(Dispatchers.IO) {
            val trickPlayManifest = database.getTrickPlayManifest(itemId)
            if (trickPlayManifest != null)
                return@withContext trickPlayManifest.toTrickPlayManifest()
            // https://github.com/nicknsy/jellyscrub/blob/main/Nick.Plugin.Jellyscrub/Api/TrickplayController.cs
            val pathParameters = mutableMapOf<String, UUID>()
            pathParameters["itemId"] = itemId

            try {
                return@withContext jellyfinApi.api.get<TrickPlayManifest>(
                    "/Trickplay/{itemId}/GetManifest",
                    pathParameters
                ).content
            } catch (e: Exception) {
                return@withContext null
            }
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

            // https://github.com/nicknsy/jellyscrub/blob/main/Nick.Plugin.Jellyscrub/Api/TrickplayController.cs
            val pathParameters = mutableMapOf<String, Any>()
            pathParameters["itemId"] = itemId
            pathParameters["width"] = width

            try {
                return@withContext jellyfinApi.api.get<ByteReadChannel>(
                    "/Trickplay/{itemId}/{width}/GetBIF",
                    pathParameters
                ).content.toByteArray()
            } catch (e: Exception) {
                return@withContext null
            }
        }

    override suspend fun postCapabilities() {
        Timber.d("Sending capabilities")
        withContext(Dispatchers.IO) {
            jellyfinApi.sessionApi.postCapabilities(
                playableMediaTypes = listOf("Video"),
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
                    GeneralCommandType.PLAY_MEDIA_SOURCE
                ),
                supportsMediaControl = true
            )
        }
    }

    override suspend fun postPlaybackStart(itemId: UUID) {
        Timber.d("Sending start $itemId")
        withContext(Dispatchers.IO) {
            jellyfinApi.playStateApi.onPlaybackStart(jellyfinApi.userId!!, itemId)
        }
    }

    override suspend fun postPlaybackStop(
        itemId: UUID,
        positionTicks: Long,
        playedPercentage: Int
    ) {
        Timber.d("Sending stop $itemId")
        withContext(Dispatchers.IO) {
            when {
                playedPercentage < 10 -> {
                    database.setPlaybackPositionTicks(itemId, jellyfinApi.userId!!, 0)
                    database.setPlayed(itemId, false)
                }
                playedPercentage > 90 -> {
                    database.setPlaybackPositionTicks(itemId, jellyfinApi.userId!!, 0)
                    database.setPlayed(itemId, true)
                }
                else -> database.setPlaybackPositionTicks(itemId, jellyfinApi.userId!!, positionTicks)
            }
            jellyfinApi.playStateApi.onPlaybackStopped(
                jellyfinApi.userId!!,
                itemId,
                positionTicks = positionTicks
            )
        }
    }

    override suspend fun postPlaybackProgress(
        itemId: UUID,
        positionTicks: Long,
        isPaused: Boolean
    ) {
        Timber.d("Posting progress of $itemId, position: $positionTicks")
        withContext(Dispatchers.IO) {
            database.setPlaybackPositionTicks(itemId, jellyfinApi.userId!!, positionTicks)
            jellyfinApi.playStateApi.onPlaybackProgress(
                jellyfinApi.userId!!,
                itemId,
                positionTicks = positionTicks,
                isPaused = isPaused
            )
        }
    }

    override suspend fun markAsFavorite(itemId: UUID) {
        withContext(Dispatchers.IO) {
            jellyfinApi.userLibraryApi.markFavoriteItem(jellyfinApi.userId!!, itemId)
        }
    }

    override suspend fun unmarkAsFavorite(itemId: UUID) {
        withContext(Dispatchers.IO) {
            jellyfinApi.userLibraryApi.unmarkFavoriteItem(jellyfinApi.userId!!, itemId)
        }
    }

    override suspend fun markAsPlayed(itemId: UUID) {
        withContext(Dispatchers.IO) {
            database.setPlayed(itemId, true)
            jellyfinApi.playStateApi.markPlayedItem(jellyfinApi.userId!!, itemId)
        }
    }

    override suspend fun markAsUnplayed(itemId: UUID) {
        withContext(Dispatchers.IO) {
            database.setPlayed(itemId, false)
            jellyfinApi.playStateApi.markUnplayedItem(jellyfinApi.userId!!, itemId)
        }
    }

    override suspend fun getIntros(itemId: UUID): List<FindroidIntro> =
        withContext(Dispatchers.IO) {
            jellyfinApi.userLibraryApi.getIntros(
                jellyfinApi.userId!!,
                itemId
            ).content.items.orEmpty().map { it.toFindroidIntro(this@JellyfinRepositoryImpl) }
        }

    override fun getBaseUrl() = jellyfinApi.api.baseUrl.orEmpty()

    override suspend fun updateDeviceName(name: String) {
        jellyfinApi.jellyfin.deviceInfo?.id?.let { id ->
            withContext(Dispatchers.IO) {
                jellyfinApi.devicesApi.updateDeviceOptions(
                    id,
                    DeviceOptionsDto(0, customName = name)
                )
            }
        }
    }

    override suspend fun getUserConfiguration(): UserConfiguration = withContext(Dispatchers.IO) {
        jellyfinApi.userApi.getCurrentUser().content.configuration!!
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
