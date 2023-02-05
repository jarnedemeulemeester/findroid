package dev.jdtech.jellyfin.viewmodels

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.database.DownloadDatabaseDao
import dev.jdtech.jellyfin.models.AudioChannel
import dev.jdtech.jellyfin.models.AudioCodec
import dev.jdtech.jellyfin.models.DisplayProfile
import dev.jdtech.jellyfin.models.PlayerItem
import dev.jdtech.jellyfin.models.Resolution
import dev.jdtech.jellyfin.models.VideoMetadata
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.utils.canRetryDownload
import dev.jdtech.jellyfin.utils.deleteDownloadedEpisode
import dev.jdtech.jellyfin.utils.downloadMetadataToBaseItemDto
import dev.jdtech.jellyfin.utils.isItemAvailable
import dev.jdtech.jellyfin.utils.isItemDownloaded
import dev.jdtech.jellyfin.utils.requestDownload
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.model.api.*
import timber.log.Timber

@HiltViewModel
class MediaInfoViewModel
@Inject
constructor(
    private val application: Application,
    private val jellyfinRepository: JellyfinRepository,
    private val downloadDatabase: DownloadDatabaseDao,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    sealed class UiState {
        data class Normal(
            val item: BaseItemDto,
            val actors: List<BaseItemPerson>,
            val director: BaseItemPerson?,
            val writers: List<BaseItemPerson>,
            val videoMetadata: VideoMetadata?,
            val writersString: String,
            val genresString: String,
            val videoString: String,
            val audioString: String,
            val subtitleString: String,
            val runTime: String,
            val dateString: String,
            val nextUp: BaseItemDto?,
            val seasons: List<BaseItemDto>,
            val played: Boolean,
            val favorite: Boolean,
            val canPlay: Boolean,
            val canDownload: Boolean,
            val downloaded: Boolean,
            var canRetry: Boolean = false,
            val available: Boolean,
        ) : UiState()

        object Loading : UiState()
        data class Error(val error: Exception) : UiState()
    }

    var item: BaseItemDto? = null
    private var actors: List<BaseItemPerson> = emptyList()
    private var director: BaseItemPerson? = null
    private var writers: List<BaseItemPerson> = emptyList()
    private var videoMetadata: VideoMetadata? = null
    private var writersString: String = ""
    private var genresString: String = ""
    private var videoString: String = ""
    private var audioString: String = ""
    private var subtitleString: String = ""
    private var runTime: String = ""
    private var dateString: String = ""
    var nextUp: BaseItemDto? = null
    var seasons: List<BaseItemDto> = emptyList()
    var played: Boolean = false
    var favorite: Boolean = false
    private var canPlay: Boolean = true
    private var canDownload: Boolean = false
    private var downloaded: Boolean = false
    var canRetry: Boolean = false
    private var available: Boolean = true

    lateinit var playerItem: PlayerItem

    fun loadData(itemId: UUID, itemType: BaseItemKind) {
        viewModelScope.launch {
            _uiState.emit(UiState.Loading)
            try {
                val tempItem = jellyfinRepository.getItem(itemId)
                item = tempItem
                actors = getActors(tempItem)
                director = getDirector(tempItem)
                writers = getWriters(tempItem)
                writersString = writers.joinToString(separator = ", ") { it.name.toString() }
                videoMetadata = if (tempItem.type == BaseItemKind.MOVIE) parseVideoMetadata(tempItem) else null
                genresString = tempItem.genres?.joinToString(separator = ", ") ?: ""
                videoString = getMediaString(tempItem, MediaStreamType.VIDEO)
                audioString = getMediaString(tempItem, MediaStreamType.AUDIO)
                subtitleString = getMediaString(tempItem, MediaStreamType.SUBTITLE)
                runTime = "${tempItem.runTimeTicks?.div(600000000)} min"
                dateString = getDateString(tempItem)
                played = tempItem.userData?.played ?: false
                favorite = tempItem.userData?.isFavorite ?: false
                canPlay = tempItem.playAccess != PlayAccess.NONE
                canDownload = tempItem.canDownload == true
                downloaded = isItemDownloaded(downloadDatabase, itemId)
                if (itemType == BaseItemKind.SERIES) {
                    nextUp = getNextUp(itemId)
                    seasons = jellyfinRepository.getSeasons(itemId)
                }
                _uiState.emit(
                    UiState.Normal(
                        tempItem,
                        actors,
                        director,
                        writers,
                        videoMetadata,
                        writersString,
                        genresString,
                        videoString,
                        audioString,
                        subtitleString,
                        runTime,
                        dateString,
                        nextUp,
                        seasons,
                        played,
                        favorite,
                        canPlay,
                        canDownload,
                        downloaded,
                        canRetry,
                        available
                    )
                )
            } catch (e: Exception) {
                _uiState.emit(UiState.Error(e))
            }
        }
    }

    fun loadData(pItem: PlayerItem) {
        viewModelScope.launch {
            playerItem = pItem
            val tempItem = downloadMetadataToBaseItemDto(playerItem.item!!)
            item = tempItem
            actors = getActors(tempItem)
            director = getDirector(tempItem)
            writers = getWriters(tempItem)
            writersString = writers.joinToString(separator = ", ") { it.name.toString() }
            videoMetadata = if (tempItem.type == BaseItemKind.MOVIE) parseVideoMetadata(tempItem) else null
            genresString = tempItem.genres?.joinToString(separator = ", ") ?: ""
            videoString = getMediaString(tempItem, MediaStreamType.VIDEO)
            audioString = getMediaString(tempItem, MediaStreamType.AUDIO)
            subtitleString = getMediaString(tempItem, MediaStreamType.SUBTITLE)
            runTime = ""
            dateString = ""
            played = tempItem.userData?.played ?: false
            favorite = tempItem.userData?.isFavorite ?: false
            available = isItemAvailable(tempItem.id)
            canRetry = canRetryDownload(tempItem.id, downloadDatabase, application)
            _uiState.emit(
                UiState.Normal(
                    tempItem,
                    actors,
                    director,
                    writers,
                    videoMetadata,
                    writersString,
                    genresString,
                    videoString,
                    audioString,
                    subtitleString,
                    runTime,
                    dateString,
                    nextUp,
                    seasons,
                    played,
                    favorite,
                    canPlay,
                    canDownload,
                    downloaded,
                    canRetry,
                    available
                )
            )
        }
    }

    private suspend fun getActors(item: BaseItemDto): List<BaseItemPerson> {
        val actors: List<BaseItemPerson>
        withContext(Dispatchers.Default) {
            actors = item.people?.filter { it.type == "Actor" } ?: emptyList()
        }
        return actors
    }

    private suspend fun getDirector(item: BaseItemDto): BaseItemPerson? {
        val director: BaseItemPerson?
        withContext(Dispatchers.Default) {
            director = item.people?.firstOrNull { it.type == "Director" }
        }
        return director
    }

    private suspend fun getWriters(item: BaseItemDto): List<BaseItemPerson> {
        val writers: List<BaseItemPerson>
        withContext(Dispatchers.Default) {
            writers = item.people?.filter { it.type == "Writer" } ?: emptyList()
        }
        return writers
    }

    private suspend fun getMediaString(item: BaseItemDto, type: MediaStreamType): String {
        val streams: List<MediaStream>
        withContext(Dispatchers.Default) {
            streams = item.mediaStreams?.filter { it.type == type } ?: emptyList()
        }
        return streams.map { it.displayTitle }.joinToString(separator = ", ")
    }

    private suspend fun parseVideoMetadata(item: BaseItemDto): VideoMetadata {
        val resolution = mutableListOf<Resolution>()
        val audioChannels = mutableListOf<AudioChannel>()
        val displayProfile = mutableListOf<DisplayProfile>()
        val audioCodecs = mutableListOf<AudioCodec>()
        val isAtmosAudio = mutableListOf<Boolean>()

        withContext(Dispatchers.Default) {
            item.mediaStreams?.filter { stream ->
                when (stream.type) {
                    MediaStreamType.AUDIO -> {
                        /**
                         * Match audio profile from [MediaStream.channelLayout]
                         */
                        audioChannels.add(
                            when (stream.channelLayout) {
                                AudioChannel.CH_2_1.raw -> AudioChannel.CH_2_1
                                AudioChannel.CH_5_1.raw -> AudioChannel.CH_5_1
                                AudioChannel.CH_7_1.raw -> AudioChannel.CH_7_1
                                else -> AudioChannel.CH_2_0
                            }
                        )

                        /**
                         * Match [MediaStream.displayTitle] for Dolby Atmos
                         */
                        stream.displayTitle?.apply {
                            isAtmosAudio.add(contains("ATMOS", true))
                        }

                        /**
                         * Match audio codec from [MediaStream.codec]
                         */
                        audioCodecs.add(
                            when (stream.codec?.lowercase()) {
                                AudioCodec.FLAC.toString() -> AudioCodec.FLAC
                                AudioCodec.AAC.toString() -> AudioCodec.AAC
                                AudioCodec.AC3.toString() -> AudioCodec.AC3
                                AudioCodec.EAC3.toString() -> AudioCodec.EAC3
                                AudioCodec.VORBIS.toString() -> AudioCodec.VORBIS
                                AudioCodec.OPUS.toString() -> AudioCodec.OPUS
                                AudioCodec.TRUEHD.toString() -> AudioCodec.TRUEHD
                                AudioCodec.DTS.toString() -> AudioCodec.DTS
                                else -> AudioCodec.MP3
                            }
                        )
                        true
                    }

                    MediaStreamType.VIDEO -> {
                        with(stream) {
                            /**
                             * Match dynamic range from [MediaStream.videoRangeType]
                             */
                            displayProfile.add(
                                when (videoRangeType) {
                                    DisplayProfile.HDR.raw -> DisplayProfile.HDR
                                    DisplayProfile.HDR10.raw -> DisplayProfile.HDR10
                                    DisplayProfile.DOLBY_VISION.raw -> DisplayProfile.DOLBY_VISION
                                    DisplayProfile.HLG.raw -> DisplayProfile.HLG
                                    else -> DisplayProfile.SDR
                                }
                            )

                            /**
                             * Force stream [MediaStream.height] and [MediaStream.width] as not null
                             * since we are inside [MediaStreamType.VIDEO] block
                             */
                            resolution.add(
                                when {
                                    height!! <= 1080 && width!! <= 1920 -> {
                                        Resolution.HD
                                    }

                                    height!! <= 2160 && width!! <= 3840 -> {
                                        Resolution.UHD
                                    }

                                    else -> Resolution.SD
                                }
                            )
                        }
                        true
                    }

                    else -> false
                }
            }
        }

        return VideoMetadata(
            resolution,
            displayProfile.toSet().toList(),
            audioChannels.toSet().toList(),
            audioCodecs.toSet().toList(),
            isAtmosAudio
        )
    }

    private suspend fun getNextUp(seriesId: UUID): BaseItemDto? {
        val nextUpItems = jellyfinRepository.getNextUp(seriesId)
        return if (nextUpItems.isNotEmpty()) {
            nextUpItems[0]
        } else {
            null
        }
    }

    fun markAsPlayed(itemId: UUID) {
        viewModelScope.launch {
            try {
                jellyfinRepository.markAsPlayed(itemId)
            } catch (e: ApiClientException) {
                Timber.d(e)
            }
        }
        played = true
    }

    fun markAsUnplayed(itemId: UUID) {
        viewModelScope.launch {
            try {
                jellyfinRepository.markAsUnplayed(itemId)
            } catch (e: ApiClientException) {
                Timber.d(e)
            }
        }
        played = false
    }

    fun markAsFavorite(itemId: UUID) {
        viewModelScope.launch {
            try {
                jellyfinRepository.markAsFavorite(itemId)
            } catch (e: ApiClientException) {
                Timber.d(e)
            }
        }
        favorite = true
    }

    fun unmarkAsFavorite(itemId: UUID) {
        viewModelScope.launch {
            try {
                jellyfinRepository.unmarkAsFavorite(itemId)
            } catch (e: ApiClientException) {
                Timber.d(e)
            }
        }
        favorite = false
    }

    private fun getDateString(item: BaseItemDto): String {
        val dateRange: MutableList<String> = mutableListOf()
        item.productionYear?.let { dateRange.add(it.toString()) }
        when (item.status) {
            "Continuing" -> {
                dateRange.add("Present")
            }

            "Ended" -> {
                item.endDate?.let { dateRange.add(it.year.toString()) }
            }
        }
        if (dateRange.count() > 1 && dateRange[0] == dateRange[1]) return dateRange[0]
        return dateRange.joinToString(separator = " - ")
    }

    fun download() {
        viewModelScope.launch {
            requestDownload(
                jellyfinRepository,
                downloadDatabase,
                application,
                item!!.id
            )
        }
    }

    fun deleteItem() {
        deleteDownloadedEpisode(downloadDatabase, playerItem.itemId)
    }
}
