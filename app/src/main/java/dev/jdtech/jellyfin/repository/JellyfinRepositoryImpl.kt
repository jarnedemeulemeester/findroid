package dev.jdtech.jellyfin.repository

import dev.jdtech.jellyfin.api.JellyfinApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.model.api.*
import timber.log.Timber
import java.util.*

class JellyfinRepositoryImpl(private val jellyfinApi: JellyfinApi) : JellyfinRepository {
    override suspend fun getUserViews(): List<BaseItemDto> {
        val views: List<BaseItemDto>
        withContext(Dispatchers.IO) {
            views =
                jellyfinApi.viewsApi.getUserViews(jellyfinApi.userId!!).content.items ?: listOf()
        }
        return views
    }

    override suspend fun getItem(itemId: UUID): BaseItemDto {
        val item: BaseItemDto
        withContext(Dispatchers.IO) {
            item = jellyfinApi.userLibraryApi.getItem(jellyfinApi.userId!!, itemId).content
        }
        return item
    }

    override suspend fun getItems(parentId: UUID?): List<BaseItemDto> {
        val items: List<BaseItemDto>
        withContext(Dispatchers.IO) {
            items = jellyfinApi.itemsApi.getItems(
                jellyfinApi.userId!!,
                parentId = parentId
            ).content.items ?: listOf()
        }
        return items
    }

    override suspend fun getResumeItems(): List<BaseItemDto> {
        val items: List<BaseItemDto>
        withContext(Dispatchers.IO) {
            items =
                jellyfinApi.itemsApi.getResumeItems(jellyfinApi.userId!!).content.items ?: listOf()
        }
        return items
    }

    override suspend fun getLatestMedia(parentId: UUID): List<BaseItemDto> {
        val items: List<BaseItemDto>
        withContext(Dispatchers.IO) {
            items = jellyfinApi.userLibraryApi.getLatestMedia(
                jellyfinApi.userId!!,
                parentId = parentId
            ).content
        }
        return items
    }

    override suspend fun getSeasons(seriesId: UUID): List<BaseItemDto> {
        val seasons: List<BaseItemDto>
        withContext(Dispatchers.IO) {
            seasons = jellyfinApi.showsApi.getSeasons(seriesId, jellyfinApi.userId!!).content.items
                ?: listOf()
        }
        return seasons
    }

    override suspend fun getNextUp(seriesId: UUID?): List<BaseItemDto> {
        val nextUpItems: List<BaseItemDto>
        withContext(Dispatchers.IO) {
            nextUpItems = jellyfinApi.showsApi.getNextUp(
                jellyfinApi.userId!!,
                seriesId = seriesId?.toString(),
            ).content.items ?: listOf()
        }
        return nextUpItems
    }

    override suspend fun getEpisodes(
        seriesId: UUID,
        seasonId: UUID,
        fields: List<ItemFields>?
    ): List<BaseItemDto> {
        val episodes: List<BaseItemDto>
        withContext(Dispatchers.IO) {
            episodes = jellyfinApi.showsApi.getEpisodes(
                seriesId, jellyfinApi.userId!!, seasonId = seasonId, fields = fields
            ).content.items ?: listOf()
        }
        return episodes
    }

    override suspend fun getMediaSources(itemId: UUID): List<MediaSourceInfo> {
        val mediaSourceInfoList: List<MediaSourceInfo>
        val mediaInfo by jellyfinApi.mediaInfoApi.getPostedPlaybackInfo(
            itemId, PlaybackInfoDto(
                userId = jellyfinApi.userId!!,
                deviceProfile = DeviceProfile(
                    name = "Direct play all",
                    maxStaticBitrate = 1_000_000_000,
                    maxStreamingBitrate = 1_000_000_000,
                    codecProfiles = listOf(),
                    containerProfiles = listOf(),
                    directPlayProfiles = listOf(
                        DirectPlayProfile(
                            type = DlnaProfileType.VIDEO
                        ), DirectPlayProfile(type = DlnaProfileType.AUDIO)
                    ),
                    transcodingProfiles = listOf(),
                    responseProfiles = listOf(),
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
        )
        mediaSourceInfoList = mediaInfo.mediaSources ?: listOf()
        return mediaSourceInfoList
    }

    override suspend fun getStreamUrl(itemId: UUID, mediaSourceId: String): String {
        var streamUrl = ""
        withContext(Dispatchers.IO) {
            try {
                streamUrl = jellyfinApi.videosApi.getVideoStreamUrl(
                    itemId,
                    static = true,
                    mediaSourceId = mediaSourceId
                )
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
        return streamUrl
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
}