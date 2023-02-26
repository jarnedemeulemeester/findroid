package dev.jdtech.jellyfin.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.models.AudioChannel
import dev.jdtech.jellyfin.models.AudioCodec
import dev.jdtech.jellyfin.models.DisplayProfile
import dev.jdtech.jellyfin.models.JellyfinEpisodeItem
import dev.jdtech.jellyfin.models.JellyfinMovieItem
import dev.jdtech.jellyfin.models.JellyfinSeasonItem
import dev.jdtech.jellyfin.models.Resolution
import dev.jdtech.jellyfin.models.VideoMetadata
import dev.jdtech.jellyfin.repository.JellyfinRepository
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.BaseItemPerson
import org.jellyfin.sdk.model.api.MediaStream
import org.jellyfin.sdk.model.api.MediaStreamType
import timber.log.Timber

@HiltViewModel
class MediaInfoViewModel
@Inject
constructor(
    private val jellyfinRepository: JellyfinRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    sealed class UiState {
        data class Normal(
            val item: JellyfinMovieItem,
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
            val nextUp: JellyfinEpisodeItem?,
            val seasons: List<JellyfinSeasonItem>,
        ) : UiState()

        object Loading : UiState()
        data class Error(val error: Exception) : UiState()
    }

    lateinit var item: JellyfinMovieItem
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
    var nextUp: JellyfinEpisodeItem? = null
    var seasons: List<JellyfinSeasonItem> = emptyList()

    fun loadData(itemId: UUID, itemType: BaseItemKind) {
        viewModelScope.launch {
            _uiState.emit(UiState.Loading)
            try {
                item = jellyfinRepository.getMovie(itemId)
                actors = getActors(item)
                director = getDirector(item)
                writers = getWriters(item)
                writersString = writers.joinToString(separator = ", ") { it.name.toString() }
                videoMetadata = parseVideoMetadata(item)
                genresString = item.genres.joinToString(separator = ", ")
                videoString = getMediaString(item, MediaStreamType.VIDEO)
                audioString = getMediaString(item, MediaStreamType.AUDIO)
                subtitleString = getMediaString(item, MediaStreamType.SUBTITLE)
                runTime = "${item.runtimeTicks.div(600000000)} min"
                dateString = getDateString(item)
                if (itemType == BaseItemKind.SERIES) {
                    nextUp = getNextUp(itemId)
                    seasons = jellyfinRepository.getSeasons(itemId)
                }
                _uiState.emit(
                    UiState.Normal(
                        item,
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
                    )
                )
            } catch (e: Exception) {
                _uiState.emit(UiState.Error(e))
            }
        }
    }

    private suspend fun getActors(item: JellyfinMovieItem): List<BaseItemPerson> {
        val actors: List<BaseItemPerson>
        withContext(Dispatchers.Default) {
            actors = item.people.filter { it.type == "Actor" }
        }
        return actors
    }

    private suspend fun getDirector(item: JellyfinMovieItem): BaseItemPerson? {
        val director: BaseItemPerson?
        withContext(Dispatchers.Default) {
            director = item.people.firstOrNull { it.type == "Director" }
        }
        return director
    }

    private suspend fun getWriters(item: JellyfinMovieItem): List<BaseItemPerson> {
        val writers: List<BaseItemPerson>
        withContext(Dispatchers.Default) {
            writers = item.people.filter { it.type == "Writer" }
        }
        return writers
    }

    private suspend fun getMediaString(item: JellyfinMovieItem, type: MediaStreamType): String {
        val streams: List<MediaStream>
        withContext(Dispatchers.Default) {
            streams = item.sources.getOrNull(0)?.mediaStreams?.filter { it.type == type } ?: emptyList()
        }
        return streams.map { it.displayTitle }.joinToString(separator = ", ")
    }

    private suspend fun parseVideoMetadata(item: JellyfinMovieItem): VideoMetadata {
        val resolution = mutableListOf<Resolution>()
        val audioChannels = mutableListOf<AudioChannel>()
        val displayProfile = mutableListOf<DisplayProfile>()
        val audioCodecs = mutableListOf<AudioCodec>()
        val isAtmosAudio = mutableListOf<Boolean>()

        withContext(Dispatchers.Default) {
            item.sources.getOrNull(0)?.mediaStreams?.filter { stream ->
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
                                /**
                                 * Since [MediaStream.videoRangeType] is [DisplayProfile.HDR10]
                                 * Check if [MediaStream.videoDoViTitle] is not null and return
                                 * [DisplayProfile.DOLBY_VISION] accordingly
                                 */
                                if (stream.videoDoViTitle != null) {
                                    DisplayProfile.DOLBY_VISION
                                } else when (videoRangeType) {
                                    DisplayProfile.HDR.raw -> DisplayProfile.HDR
                                    DisplayProfile.HDR10.raw -> DisplayProfile.HDR10
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

    private suspend fun getNextUp(seriesId: UUID): JellyfinEpisodeItem? {
        val nextUpItems = jellyfinRepository.getNextUp(seriesId)
        return if (nextUpItems.isNotEmpty()) {
            nextUpItems[0]
        } else {
            null
        }
    }

    fun togglePlayed() {
        viewModelScope.launch {
            try {
                if (item.played) {
                    jellyfinRepository.markAsUnplayed(item.id)
                } else {
                    jellyfinRepository.markAsPlayed(item.id)
                }
                loadData(item.id, BaseItemKind.MOVIE)
            } catch (e: ApiClientException) {
                Timber.d(e)
            }
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            try {
                if (item.favorite) {
                    jellyfinRepository.unmarkAsFavorite(item.id)
                } else {
                    jellyfinRepository.markAsFavorite(item.id)
                }
                loadData(item.id, BaseItemKind.MOVIE)
            } catch (e: ApiClientException) {
                Timber.d(e)
            }
        }
    }

    private fun getDateString(item: JellyfinMovieItem): String {
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
    }

    fun deleteItem() {
    }
}
