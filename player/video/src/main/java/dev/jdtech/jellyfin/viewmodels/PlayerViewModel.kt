package dev.jdtech.jellyfin.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MimeTypes
import com.google.android.gms.cast.framework.CastContext
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.models.ExternalSubtitle
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.FindroidMovie
import dev.jdtech.jellyfin.models.FindroidSeason
import dev.jdtech.jellyfin.models.FindroidShow
import dev.jdtech.jellyfin.models.FindroidSourceType
import dev.jdtech.jellyfin.models.PlayerItem
import dev.jdtech.jellyfin.repository.JellyfinRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.ItemFields
import org.jellyfin.sdk.model.api.MediaStreamType
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject internal constructor(
    private val repository: JellyfinRepository,
    private val jellyfinApi: JellyfinApi,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val eventsChannel = Channel<PlayerItemsEvent>()
    val eventsChannelFlow = eventsChannel.receiveAsFlow()

    fun loadPlayerItems(
        item: FindroidItem,
        mediaSourceIndex: Int? = null,
    ) {
        Timber.d("Loading player items for item ${item.id}")

        viewModelScope.launch {
            val session = CastContext.getSharedInstance(context).sessionManager.currentCastSession

            if (session != null) {
                val thing =
                    "{\"options\":{\"ids\":[\"${item.id}\"],\"startPositionTicks\":${
                        item.playbackPositionTicks
                    },\"serverId\":\"\",\"fullscreen\":true,\"items\":[{\"Id\":\"${item.id}\",\"ServerId\":\"\",\"Name\":\"${item.name}\",\"Type\":\"${item.sources.first().type}\",\"MediaType\":\"${item.sources.first().mediaStreams.first().type}\",\"IsFolder\":false}]},\"command\":\"PlayNow\",\"userId\":\"${jellyfinApi.userId}\",\"deviceId\":\"${jellyfinApi.api.deviceInfo.id}\",\"accessToken\":\"${jellyfinApi.api.accessToken}\",\"serverAddress\":\"${jellyfinApi.api.baseUrl}\",\"serverId\":\"\",\"serverVersion\":\"\",\"receiverName\":\"Living Room TV\",\"subtitleAppearance\":{\"verticalPosition\":-3},\"subtitleBurnIn\":\"\"}"
                session.sendMessage("urn:x-cast:com.connectsdk", thing)
                return@launch
            }

            val playbackPosition = item.playbackPositionTicks.div(10000)

            try {
                val items = prepareMediaPlayerItems(item, playbackPosition, mediaSourceIndex)
                eventsChannel.send(PlayerItemsEvent.PlayerItemsReady(items))
            } catch (e: Exception) {
                Timber.d(e)
                eventsChannel.send(PlayerItemsEvent.PlayerItemsError(e))
            }

//            playerItems.tryEmit(items)
        }
    }

    private suspend fun prepareMediaPlayerItems(
        item: FindroidItem,
        playbackPosition: Long,
        mediaSourceIndex: Int?,
    ): List<PlayerItem> = when (item) {
        is FindroidMovie -> movieToPlayerItem(item, playbackPosition, mediaSourceIndex)
        is FindroidShow -> seriesToPlayerItems(item, playbackPosition, mediaSourceIndex)
        is FindroidSeason -> seasonToPlayerItems(item, playbackPosition, mediaSourceIndex)
        is FindroidEpisode -> episodeToPlayerItems(item, playbackPosition, mediaSourceIndex)
        else -> emptyList()
    }

    private suspend fun movieToPlayerItem(
        item: FindroidMovie,
        playbackPosition: Long,
        mediaSourceIndex: Int?,
    ) = listOf(item.toPlayerItem(mediaSourceIndex, playbackPosition))

    private suspend fun seriesToPlayerItems(
        item: FindroidShow,
        playbackPosition: Long,
        mediaSourceIndex: Int?,
    ): List<PlayerItem> {
        val nextUp = repository.getNextUp(item.id)

        return if (nextUp.isEmpty()) {
            repository
                .getSeasons(item.id)
                .flatMap { seasonToPlayerItems(it, playbackPosition, mediaSourceIndex) }
        } else {
            episodeToPlayerItems(nextUp.first(), playbackPosition, mediaSourceIndex)
        }
    }

    private suspend fun seasonToPlayerItems(
        item: FindroidSeason,
        playbackPosition: Long,
        mediaSourceIndex: Int?,
    ): List<PlayerItem> {
        return repository
            .getEpisodes(
                seriesId = item.seriesId,
                seasonId = item.id,
                fields = listOf(ItemFields.MEDIA_SOURCES),
            )
            .filter { it.sources.isNotEmpty() }
            .filter { !it.missing }
            .map { episode -> episode.toPlayerItem(mediaSourceIndex, playbackPosition) }
    }

    private suspend fun episodeToPlayerItems(
        item: FindroidEpisode,
        playbackPosition: Long,
        mediaSourceIndex: Int?,
    ): List<PlayerItem> {
        // TODO Move user configuration to a separate class
        val userConfig = try {
            repository.getUserConfiguration()
        } catch (_: Exception) {
            null
        }
        return repository
            .getEpisodes(
                seriesId = item.seriesId,
                seasonId = item.seasonId,
                fields = listOf(ItemFields.MEDIA_SOURCES),
                startItemId = item.id,
                limit = if (userConfig?.enableNextEpisodeAutoPlay != false) null else 1,
            )
            .filter { it.sources.isNotEmpty() }
            .filter { !it.missing }
            .map { episode -> episode.toPlayerItem(mediaSourceIndex, playbackPosition) }
    }

    private suspend fun FindroidItem.toPlayerItem(
        mediaSourceIndex: Int?,
        playbackPosition: Long,
    ): PlayerItem {
        val mediaSources = repository.getMediaSources(id, true)
        val mediaSource = if (mediaSourceIndex == null) {
            mediaSources.firstOrNull { it.type == FindroidSourceType.LOCAL } ?: mediaSources[0]
        } else {
            mediaSources[mediaSourceIndex]
        }
        val externalSubtitles = mediaSource.mediaStreams
            .filter { mediaStream ->
                mediaStream.isExternal && mediaStream.type == MediaStreamType.SUBTITLE && !mediaStream.path.isNullOrBlank()
            }
            .map { mediaStream ->
                // Temp fix for vtt
                // Jellyfin returns a srt stream when it should return vtt stream.
                var deliveryUrl = mediaStream.path!!
                if (mediaStream.codec == "webvtt") {
                    deliveryUrl = deliveryUrl.replace("Stream.srt", "Stream.vtt")
                }

                ExternalSubtitle(
                    mediaStream.title,
                    mediaStream.language,
                    Uri.parse(deliveryUrl),
                    when (mediaStream.codec) {
                        "subrip" -> MimeTypes.APPLICATION_SUBRIP
                        "webvtt" -> MimeTypes.TEXT_VTT
                        "ass" -> MimeTypes.TEXT_SSA
                        else -> MimeTypes.TEXT_UNKNOWN
                    },
                )
            }
        return PlayerItem(
            name = name,
            itemId = id,
            mediaSourceId = mediaSource.id,
            mediaSourceUri = mediaSource.path,
            playbackPosition = playbackPosition,
            parentIndexNumber = if (this is FindroidEpisode) parentIndexNumber else null,
            indexNumber = if (this is FindroidEpisode) indexNumber else null,
            indexNumberEnd = if (this is FindroidEpisode) indexNumberEnd else null,
            externalSubtitles = externalSubtitles,
        )
    }
}

sealed interface PlayerItemsEvent {
    data class PlayerItemsReady(val items: List<PlayerItem>) : PlayerItemsEvent
    data class PlayerItemsError(val error: Exception) : PlayerItemsEvent
}
