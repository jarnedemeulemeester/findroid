package dev.jdtech.jellyfin.repository

import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.utils.SortBy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.model.api.*
import timber.log.Timber
import java.util.*

class JellyfinRepositoryImpl(private val jellyfinApi: JellyfinApi) : JellyfinRepository {
    override suspend fun getUserViews(): List<BaseItemDto> = withContext(Dispatchers.IO) {
        jellyfinApi.viewsApi.getUserViews(jellyfinApi.userId!!).content.items.orEmpty()
    }

    override suspend fun getItem(itemId: UUID): BaseItemDto = withContext(Dispatchers.IO) {
        jellyfinApi.userLibraryApi.getItem(jellyfinApi.userId!!, itemId).content
    }

    override suspend fun getItems(
        parentId: UUID?,
        includeTypes: List<BaseItemKind>?,
        recursive: Boolean,
        sortBy: SortBy,
        sortOrder: SortOrder
    ): List<BaseItemDto> = withContext(Dispatchers.IO) {
        jellyfinApi.itemsApi.getItems(
            jellyfinApi.userId!!,
            parentId = parentId,
            includeItemTypes = includeTypes,
            recursive = recursive,
            sortBy = listOf(sortBy.SortString),
            sortOrder = listOf(sortOrder)
        ).content.items.orEmpty()
    }

    override suspend fun getPersonItems(
        personIds: List<UUID>,
        includeTypes: List<BaseItemKind>?,
        recursive: Boolean
    ): List<BaseItemDto> = withContext(Dispatchers.IO) {
        jellyfinApi.itemsApi.getItems(
            jellyfinApi.userId!!,
            personIds = personIds,
            includeItemTypes = includeTypes,
            recursive = recursive
        ).content.items.orEmpty()
    }

    override suspend fun getFavoriteItems(): List<BaseItemDto> = withContext(Dispatchers.IO) {
        jellyfinApi.itemsApi.getItems(
            jellyfinApi.userId!!,
            filters = listOf(ItemFilter.IS_FAVORITE),
            includeItemTypes = listOf(BaseItemKind.MOVIE, BaseItemKind.SERIES, BaseItemKind.EPISODE),
            recursive = true
        ).content.items.orEmpty()
    }

    override suspend fun getSearchItems(searchQuery: String): List<BaseItemDto> =
        withContext(Dispatchers.IO) {
            jellyfinApi.itemsApi.getItems(
                jellyfinApi.userId!!,
                searchTerm = searchQuery,
                includeItemTypes = listOf(BaseItemKind.MOVIE, BaseItemKind.SERIES, BaseItemKind.EPISODE),
                recursive = true
            ).content.items.orEmpty()
        }

    override suspend fun getResumeItems(): List<BaseItemDto> = withContext(Dispatchers.IO) {
        jellyfinApi.itemsApi.getResumeItems(
            jellyfinApi.userId!!,
            includeItemTypes = listOf(BaseItemKind.MOVIE, BaseItemKind.EPISODE),
        ).content.items.orEmpty()
    }

    override suspend fun getLatestMedia(parentId: UUID): List<BaseItemDto> =
        withContext(Dispatchers.IO) {
            jellyfinApi.userLibraryApi.getLatestMedia(
                jellyfinApi.userId!!,
                parentId = parentId
            ).content
        }

    override suspend fun getSeasons(seriesId: UUID): List<BaseItemDto> =
        withContext(Dispatchers.IO) {
            jellyfinApi.showsApi.getSeasons(seriesId, jellyfinApi.userId!!).content.items.orEmpty()
        }

    override suspend fun getNextUp(seriesId: UUID?): List<BaseItemDto> =
        withContext(Dispatchers.IO) {
            jellyfinApi.showsApi.getNextUp(
                jellyfinApi.userId!!,
                seriesId = seriesId?.toString(),
            ).content.items.orEmpty()
        }

    override suspend fun getEpisodes(
        seriesId: UUID,
        seasonId: UUID,
        fields: List<ItemFields>?,
        startItemId: UUID?
    ): List<BaseItemDto> = withContext(Dispatchers.IO) {
        jellyfinApi.showsApi.getEpisodes(
            seriesId,
            jellyfinApi.userId!!,
            seasonId = seasonId,
            fields = fields,
            startItemId = startItemId
        ).content.items.orEmpty()
    }

    override suspend fun getMediaSources(itemId: UUID): List<MediaSourceInfo> =
        withContext(Dispatchers.IO) {
            jellyfinApi.mediaInfoApi.getPostedPlaybackInfo(
                itemId, PlaybackInfoDto(
                    userId = jellyfinApi.userId!!,
                    deviceProfile = DeviceProfile(
                        name = "Direct play all",
                        maxStaticBitrate = 1_000_000_000,
                        maxStreamingBitrate = 1_000_000_000,
                        codecProfiles = emptyList(),
                        containerProfiles = emptyList(),
                        directPlayProfiles = listOf(
                            DirectPlayProfile(
                                type = DlnaProfileType.VIDEO
                            ), DirectPlayProfile(type = DlnaProfileType.AUDIO)
                        ),
                        transcodingProfiles = emptyList(),
                        responseProfiles = emptyList(),
                        subtitleProfiles = emptyList(),
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
                    startTimeTicks = null,
                    audioStreamIndex = null,
                    subtitleStreamIndex = null,
                    maxStreamingBitrate = 1_000_000_000,
                )
            ).content.mediaSources
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
                ), supportsMediaControl = true
            )
        }
    }

    override suspend fun postPlaybackStart(itemId: UUID) {
        Timber.d("Sending start $itemId")
        withContext(Dispatchers.IO) {
            jellyfinApi.playStateApi.onPlaybackStart(jellyfinApi.userId!!, itemId)
        }
    }

    override suspend fun postPlaybackStop(itemId: UUID, positionTicks: Long) {
        Timber.d("Sending stop $itemId")
        withContext(Dispatchers.IO) {
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
            jellyfinApi.playStateApi.markPlayedItem(jellyfinApi.userId!!, itemId)
        }
    }

    override suspend fun markAsUnplayed(itemId: UUID) {
        withContext(Dispatchers.IO) {
            jellyfinApi.playStateApi.markUnplayedItem(jellyfinApi.userId!!, itemId)
        }
    }

    override suspend fun getIntros(itemId: UUID): List<BaseItemDto> = withContext(Dispatchers.IO) {
        jellyfinApi.userLibraryApi.getIntros(jellyfinApi.userId!!, itemId).content.items.orEmpty()
    }
}