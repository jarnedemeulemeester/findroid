package dev.jdtech.jellyfin.viewmodels

import android.content.Context
import android.net.Uri
import android.widget.Spinner
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MimeTypes
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaTrack
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.google.android.gms.common.images.WebImage
import dagger.hilt.android.lifecycle.HiltViewModel
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.jellyfin.sdk.api.client.extensions.hlsSegmentApi
import org.jellyfin.sdk.api.client.extensions.imageApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.ImageType.PRIMARY
import org.jellyfin.sdk.model.api.ItemFields
import org.jellyfin.sdk.model.api.MediaStreamType
import timber.log.Timber
import javax.inject.Inject


@HiltViewModel
class PlayerViewModel @Inject internal constructor(
    private val repository: JellyfinRepository,
    private val jellyfinApi: JellyfinApi,

) : ViewModel() {

    private val progressListeners: MutableList<MyProgressListener> = mutableListOf()
    private lateinit var spinner: Spinner

    private val playerItems = MutableSharedFlow<PlayerItemState>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    fun onPlaybackRequested(scope: LifecycleCoroutineScope, collector: (PlayerItemState) -> Unit) {
        scope.launch { playerItems.collect { collector(it) } }
    }

    fun loadPlayerItems(
        item: FindroidItem,
        mediaSourceIndex: Int? = null,
    ) {
        Timber.d("Loading player items for item ${item.id}")

        viewModelScope.launch {
            val playbackPosition = item.playbackPositionTicks.div(10000)

            val items = try {
                prepareMediaPlayerItems(item, playbackPosition, mediaSourceIndex).let(::PlayerItems)
            } catch (e: Exception) {
                Timber.d(e)
                PlayerItemError(e)
            }

            playerItems.tryEmit(items)
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
                mediaStream.type == MediaStreamType.SUBTITLE && !mediaStream.path.isNullOrBlank()
            }
            .map { mediaStream ->
                // Temp fix for vtt
                // Jellyfin returns a srt stream when it should return vtt stream.
                var deliveryUrl = mediaStream.path!!
                if (mediaStream.codec == "ass") {
                    deliveryUrl = deliveryUrl.replace("Stream.ass", "Stream.vtt")
                }
                if (mediaStream.codec == "srt") {
                    deliveryUrl = deliveryUrl.replace("Stream.srt", "Stream.srt")
                }

                ExternalSubtitle(
                    mediaStream.title,
                    mediaStream.language,
                    Uri.parse(deliveryUrl),
                    when (mediaStream.codec) {
                        "subrip" -> MimeTypes.APPLICATION_SUBRIP
                        "webvtt" -> MimeTypes.TEXT_VTT
                        "ass" -> MimeTypes.TEXT_VTT
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

    sealed class PlayerItemState

    data class PlayerItemError(val error: Exception) : PlayerItemState()
    data class PlayerItems(val items: List<PlayerItem>) : PlayerItemState()

    class MyProgressListener(
        private val test: JellyfinApi,
        private val item: PlayerItem,
        private val repository: JellyfinRepository,
        private val remoteMediaClient: RemoteMediaClient,
    ) : RemoteMediaClient.ProgressListener {

        private val progressScope =
            CoroutineScope(Dispatchers.Main) // Manually create a coroutine scope

        override fun onProgressUpdated(progress: Long, duration: Long) {
            progressScope.launch {
                try {
                    repository.postPlaybackProgress(
                        item.itemId,
                        progress * 10000,
                        remoteMediaClient.isPaused,
                    )
                } catch (e: Exception) {
                    Timber.e(e)
                }
            }
        }
    }

    private fun loadRemoteMedia(
        position: Int,
        mCastSession: CastSession,
        mediaInfo: MediaInfo,
        streamUrl: String,
        item: PlayerItem,
        episode: BaseItemDto,
    ) {
        if (mCastSession == null) {
            return
        }

        var streamPosition = 1000
        if(position > 0){
            streamPosition = position
        }


        val remoteMediaClient = mCastSession.remoteMediaClient ?: return
        var previousSubtitleTrackIds: LongArray? = null
        var newIndex = -1
        var subtitleIndex = -1
        var newAudioIndex = 1
        var externalSubs = false //used to determine if we burn in subs or not.

        var match = Regex("PlaySessionId=([\\w&]+)&").find(streamUrl)
        var playSessionId = match!!.groupValues[1]

        val callback = object : RemoteMediaClient.Callback() {

            override fun onSendingRemoteMediaRequest() {

                val test = remoteMediaClient.approximateStreamPosition
                viewModelScope.launch {
                    try {
                        repository.postPlaybackStop(
                            item.itemId,
                            test.times(10000),
                            80,
                        )
                    } catch (e: Exception) {
                        Timber.e(e)
                    }
                }
            }

            override fun onStatusUpdated() {
                val mediaStatus = remoteMediaClient.mediaStatus
                val activeSubtitleTrackIds = mediaStatus?.activeTrackIds
                val subtitlesOffset =
                    mediaInfo?.mediaTracks!!.size - item.externalSubtitles.size
                val mediaInfo = mediaStatus?.mediaInfo
                val externalSubtitleCount = mediaInfo?.textTrackStyle?.describeContents()
                var newAudioIndex = -1
                var subtitleIndex = -1
                var newUrl = ""
                if (mediaStatus != null) {
                    if (!previousSubtitleTrackIds.contentEquals(mediaStatus.activeTrackIds) && previousSubtitleTrackIds != null) {
                        if (activeSubtitleTrackIds != null) {
                            if (activeSubtitleTrackIds.isNotEmpty()) {
                                /*newIndex =
                                    (mediaStatus.activeTrackIds!!.get(0)).toInt()
                                if (newIndex < subtitlesOffset) {
                                    newAudioIndex = newIndex
                                } else {
                                    subtitleIndex = newIndex
                                }*/

                                for (track in activeSubtitleTrackIds) {
                                    if (mediaInfo?.mediaTracks?.get(track.toInt())?.type?.equals(
                                            MediaTrack.TYPE_AUDIO,
                                        ) == true
                                    ) {
                                        newAudioIndex = track.toInt()
                                    } else {
                                        subtitleIndex = track.toInt()
                                    }
                                }
                            }
                            if (activeSubtitleTrackIds.size > 1) {
                                if (subtitleIndex != newIndex) {
                                    subtitleIndex = mediaStatus.activeTrackIds!!.get(1).toInt()
                                } else {
                                    newAudioIndex = mediaStatus.activeTrackIds!!.get(1).toInt()
                                }
                            }
                            /*if (activeSubtitleTrackIds.isNotEmpty()) {
                                newIndex =
                                    (mediaStatus.activeTrackIds!!.get(0)).toInt()
                                if (mediaInfo!!.mediaTracks!![newIndex].type == (MediaTrack.TYPE_AUDIO)) {
                                    newAudioIndex = newIndex
                                    subtitleIndex = -1
                                } else {
                                    subtitleIndex = newIndex
                                    newAudioIndex = -1
                                }
                            }*/
                            /*val newUrl =
                                jellyfinApi.api.createUrl("/videos/" + item.itemId + "/master.m3u8?DeviceId=" + jellyfinApi.api.deviceInfo.id + "&MediaSourceId=" + item.mediaSourceId + "&VideoCodec=h264,h264&AudioCodec=mp3&AudioStreamIndex=" + newAudioIndex + "&SubtitleStreamIndex=" + subtitleIndex + "&VideoBitrate=10000000&AudioBitrate=320000&AudioSampleRate=44100&MaxFramerate=23.976025&PlaySessionId=" + (Math.random() * 10000).toInt() + "&api_key=" + jellyfinApi.api.accessToken + "&SubtitleMethod=Encode&RequireAvc=false&SegmentContainer=ts&BreakOnNonKeyFrames=False&h264-level=5&h264-videobitdepth=8&h264-profile=high&h264-audiochannels=2&aac-profile=lc&TranscodeReasons=SubtitleCodecNotSupported")
*/
                            viewModelScope.launch {
                                try {
                                    endTranscodeProcess(playSessionId, jellyfinApi)
                                } catch (e: Exception) {
                                    Timber.e(e)
                                }
                            }
                            var newUrl = mediaInfo?.contentUrl
                            var newSessionId = (Math.random() * 10000).toInt()
                            playSessionId = newSessionId.toString()
                            newUrl = newUrl!!.replace(
                                Regex("AudioStreamIndex=-?\\d+"),
                                "AudioStreamIndex=" + newAudioIndex,
                            )
                            newUrl = newUrl.replace(
                                Regex("SubtitleStreamIndex=-?\\d+"),
                                "SubtitleStreamIndex=" + subtitleIndex,
                            )
                            newUrl = newUrl.replace(
                                Regex("PlaySessionId=[\\w&]+"),
                                "PlaySessionId=" + newSessionId + "&api_key",
                            )
                            /*if(externalSubs){ newUrl = newUrl.replace(
                                Regex("SubtitleMethod=Encode"),
                                "SubtitleMethod=External",)
                            }*/

                            val newMediaInfo = buildMediaInfo(newUrl, item, episode)


                            remoteMediaClient.load(
                                MediaLoadRequestData.Builder()
                                    .setMediaInfo(newMediaInfo)
                                    .setAutoplay(true)
                                    .setActiveTrackIds(activeSubtitleTrackIds)
                                    .setCurrentTime((mediaStatus!!.streamPosition + 1000).toLong())
                                    .build(),
                            )
                        }
                    }
                }
                previousSubtitleTrackIds = mediaStatus?.activeTrackIds
            }
        }



        val myProgressListener =
            MyProgressListener(jellyfinApi, item, repository, remoteMediaClient)
        progressListeners.add(myProgressListener)
        remoteMediaClient.registerCallback(callback)
        remoteMediaClient.addProgressListener(myProgressListener, 50000)
        var load = remoteMediaClient.load(
            MediaLoadRequestData.Builder()
                .setMediaInfo(mediaInfo)
                .setAutoplay(true)
                //.setActiveTrackIds(LongArray(2))
                .setCurrentTime((streamPosition+2000 ).toLong()).build(),
        )


    }

    public suspend fun endTranscodeProcess(
        playSessionId: String,
        api: JellyfinApi
    ) {
        var r = jellyfinApi.api.hlsSegmentApi.stopEncodingProcess(jellyfinApi.api.deviceInfo.id,
            playSessionId
        )
        print(r)
    }

    private fun buildMediaInfo(
        streamUrl: String,
        item: PlayerItem,
        episode: BaseItemDto,
    ): MediaInfo {

        var externalSubs = false
        val mediaMetadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_GENERIC)
        val thumbnailUrl = episode.seasonId?.let {
            jellyfinApi.api.imageApi.getItemImageUrl(
                it,
                imageType = PRIMARY,
            )
        }
        if (thumbnailUrl != null) {
            var thumbnailImage = WebImage(Uri.parse(thumbnailUrl))
            mediaMetadata.addImage(thumbnailImage)
        } else {
            var thumbnailImage = WebImage(
                Uri.parse(
                    jellyfinApi.api.imageApi.getItemImageUrl(
                        item.itemId,
                        imageType = PRIMARY,
                    ),
                ),
            )
            mediaMetadata.addImage(thumbnailImage)
        }

        mediaMetadata.putString(MediaMetadata.KEY_TITLE, item.name)

        val mediaSubtitles = episode.mediaStreams?.mapIndexed { index, externalSubtitle ->

            MediaTrack.Builder(
                index.toLong(),
                if (externalSubtitle.type == MediaStreamType.AUDIO) {
                    MediaTrack.TYPE_AUDIO
                } else {
                    MediaTrack.TYPE_TEXT
                },
            )
                .setName(externalSubtitle.displayTitle + " " + externalSubtitle.type)

                .setLanguage(externalSubtitle.language)
                .build()
        }
        val copy = mediaSubtitles?.drop(1)
        val audioTracks: MutableList<MediaTrack> = ArrayList<MediaTrack>()
        val audioTracks2 = episode.mediaStreams?.mapIndexed { index, mediaStream ->
            if (!mediaStream.isTextSubtitleStream) {
                MediaTrack.Builder(index.toLong(), MediaTrack.TYPE_AUDIO)
                    .setName(mediaStream.title)
                    .setLanguage(mediaStream.language)
                    .build()
            }
        }

        return MediaInfo.Builder(streamUrl)
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType(MimeTypes.VIDEO_MP4)
            .setContentUrl(streamUrl)
            .setMediaTracks(mediaSubtitles)
            .setMetadata(mediaMetadata)
            .build()
    }

    fun startCast(items: Array<PlayerItem>, context: Context, groupPlay: Boolean) {

        val session = CastContext.getSharedInstance(context).sessionManager.currentCastSession
        viewModelScope.launch {
            try {
                val item = items.first()
                val streamUrl =
                    repository.getStreamCastUrl(
                        items.first().itemId,
                        items.first().mediaSourceId,
                    )
                val episode = repository.getItem(item.itemId)
                if (session != null) {
                    val mediaInfo = buildMediaInfo(streamUrl, item, episode)
                    loadRemoteMedia(
                        (item.playbackPosition).toInt(),
                        session,
                        mediaInfo,
                        streamUrl,
                        item,
                        episode,
                    )
                }
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    fun startCast(items: Array<PlayerItem>, context: Context) {


        val session = CastContext.getSharedInstance(context).sessionManager.currentCastSession
        viewModelScope.launch {
            try {
                val item = items.first()
                val streamUrl =
                    repository.getStreamCastUrl(
                        items.first().itemId,
                        items.first().mediaSourceId,
                    )
                val episode = repository.getItem(item.itemId)
                if (session != null) {
                    val mediaInfo = buildMediaInfo(streamUrl, item, episode)
                    loadRemoteMedia(
                        (item.playbackPosition).toInt(),
                        session,
                        mediaInfo,
                        streamUrl,
                        item,
                        episode,
                    )
                }
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }
}
