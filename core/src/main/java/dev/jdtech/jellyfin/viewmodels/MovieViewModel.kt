package dev.jdtech.jellyfin.viewmodels

import android.app.DownloadManager
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.models.AudioChannel
import dev.jdtech.jellyfin.models.AudioCodec
import dev.jdtech.jellyfin.models.DisplayProfile
import dev.jdtech.jellyfin.models.FindroidMediaStream
import dev.jdtech.jellyfin.models.FindroidMovie
import dev.jdtech.jellyfin.models.FindroidSourceType
import dev.jdtech.jellyfin.models.Resolution
import dev.jdtech.jellyfin.models.UiText
import dev.jdtech.jellyfin.models.VideoMetadata
import dev.jdtech.jellyfin.models.isDownloading
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.utils.Downloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.model.api.BaseItemPerson
import org.jellyfin.sdk.model.api.MediaStream
import org.jellyfin.sdk.model.api.MediaStreamType
import java.io.File
import java.util.UUID
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class MovieViewModel
@Inject
constructor(
    private val repository: JellyfinRepository,
    private val database: ServerDatabaseDao,
    private val downloader: Downloader,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _downloadStatus = MutableStateFlow(Pair(0, 0))
    val downloadStatus = _downloadStatus.asStateFlow()

    private val eventsChannel = Channel<MovieEvent>()
    val eventsChannelFlow = eventsChannel.receiveAsFlow()

    private val handler = Handler(Looper.getMainLooper())

    sealed class UiState {
        data class Normal(
            val item: FindroidMovie,
            val actors: List<BaseItemPerson>,
            val director: BaseItemPerson?,
            val writers: List<BaseItemPerson>,
            val videoMetadata: VideoMetadata,
            val writersString: String,
            val genresString: String,
            val videoString: String,
            val audioString: String,
            val subtitleString: String,
            val runTime: String,
            val dateString: String,
        ) : UiState()

        data object Loading : UiState()
        data class Error(val error: Exception) : UiState()
    }

    lateinit var item: FindroidMovie
    private var played: Boolean = false
    private var favorite: Boolean = false
    private var writers: List<BaseItemPerson> = emptyList()
    private var writersString: String = ""
    private var runTime: String = ""

    fun loadData(itemId: UUID) {
        viewModelScope.launch {
            _uiState.emit(UiState.Loading)
            try {
                item = repository.getMovie(itemId)
                played = item.played
                favorite = item.favorite
                writers = getWriters(item)
                writersString = writers.joinToString(separator = ", ") { it.name.toString() }
                runTime = "${item.runtimeTicks.div(600000000)} min"
                if (item.isDownloading()) {
                    pollDownloadProgress()
                }
                _uiState.emit(
                    UiState.Normal(
                        item,
                        getActors(item),
                        getDirector(item),
                        writers,
                        parseVideoMetadata(item),
                        writersString,
                        item.genres.joinToString(separator = ", "),
                        getMediaString(item, MediaStreamType.VIDEO),
                        getMediaString(item, MediaStreamType.AUDIO),
                        getMediaString(item, MediaStreamType.SUBTITLE),
                        runTime,
                        getDateString(item),
                    ),
                )
            } catch (_: NullPointerException) {
                // Navigate back because item does not exist (probably because it's been deleted)
                eventsChannel.send(MovieEvent.NavigateBack)
            } catch (e: Exception) {
                _uiState.emit(UiState.Error(e))
            }
        }
    }

    private suspend fun getActors(item: FindroidMovie): List<BaseItemPerson> {
        val actors: List<BaseItemPerson>
        withContext(Dispatchers.Default) {
            actors = item.people.filter { it.type == "Actor" }
        }
        return actors
    }

    private suspend fun getDirector(item: FindroidMovie): BaseItemPerson? {
        val director: BaseItemPerson?
        withContext(Dispatchers.Default) {
            director = item.people.firstOrNull { it.type == "Director" }
        }
        return director
    }

    private suspend fun getWriters(item: FindroidMovie): List<BaseItemPerson> {
        val writers: List<BaseItemPerson>
        withContext(Dispatchers.Default) {
            writers = item.people.filter { it.type == "Writer" }
        }
        return writers
    }

    private suspend fun getMediaString(item: FindroidMovie, type: MediaStreamType): String {
        val streams: List<FindroidMediaStream>
        withContext(Dispatchers.Default) {
            streams =
                item.sources.getOrNull(0)?.mediaStreams?.filter { it.type == type } ?: emptyList()
        }
        return streams.map { it.displayTitle }.joinToString(separator = ", ")
    }

    private suspend fun parseVideoMetadata(item: FindroidMovie): VideoMetadata {
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
                            },
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
                            when (stream.codec.lowercase()) {
                                AudioCodec.FLAC.toString() -> AudioCodec.FLAC
                                AudioCodec.AAC.toString() -> AudioCodec.AAC
                                AudioCodec.AC3.toString() -> AudioCodec.AC3
                                AudioCodec.EAC3.toString() -> AudioCodec.EAC3
                                AudioCodec.VORBIS.toString() -> AudioCodec.VORBIS
                                AudioCodec.OPUS.toString() -> AudioCodec.OPUS
                                AudioCodec.TRUEHD.toString() -> AudioCodec.TRUEHD
                                AudioCodec.DTS.toString() -> AudioCodec.DTS
                                else -> AudioCodec.MP3
                            },
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
                                } else {
                                    when (videoRangeType) {
                                        DisplayProfile.HDR.raw -> DisplayProfile.HDR
                                        DisplayProfile.HDR10.raw -> DisplayProfile.HDR10
                                        DisplayProfile.HLG.raw -> DisplayProfile.HLG
                                        else -> DisplayProfile.SDR
                                    }
                                },
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
                                },
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
            isAtmosAudio,
        )
    }

    fun togglePlayed(): Boolean {
        when (played) {
            false -> {
                played = true
                viewModelScope.launch {
                    try {
                        repository.markAsPlayed(item.id)
                    } catch (_: Exception) {}
                }
            }
            true -> {
                played = false
                viewModelScope.launch {
                    try {
                        repository.markAsUnplayed(item.id)
                    } catch (_: Exception) {}
                }
            }
        }
        return played
    }

    fun toggleFavorite(): Boolean {
        when (favorite) {
            false -> {
                favorite = true
                viewModelScope.launch {
                    try {
                        repository.markAsFavorite(item.id)
                    } catch (_: Exception) {}
                }
            }
            true -> {
                favorite = false
                viewModelScope.launch {
                    try {
                        repository.unmarkAsFavorite(item.id)
                    } catch (_: Exception) {}
                }
            }
        }
        return favorite
    }

    private fun getDateString(item: FindroidMovie): String {
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

    fun download(sourceIndex: Int = 0, storageIndex: Int = 0) {
        viewModelScope.launch {
            val result = downloader.downloadItem(item, item.sources[sourceIndex].id, storageIndex)

            // Send one time signal to fragment that the download has been initiated
            _downloadStatus.emit(Pair(10, Random.nextInt()))

            if (result.second != null) {
                eventsChannel.send(MovieEvent.DownloadError(result.second!!))
            }

            loadData(item.id)
        }
    }

    fun cancelDownload() {
        viewModelScope.launch {
            downloader.cancelDownload(item, item.sources.first { it.type == FindroidSourceType.LOCAL })
            loadData(item.id)
        }
    }

    fun deleteItem() {
        viewModelScope.launch {
            downloader.deleteItem(item, item.sources.first { it.type == FindroidSourceType.LOCAL })
            loadData(item.id)
        }
    }

    private fun pollDownloadProgress() {
        handler.removeCallbacksAndMessages(null)
        val downloadProgressRunnable = object : Runnable {
            override fun run() {
                viewModelScope.launch {
                    val source = item.sources.firstOrNull { it.type == FindroidSourceType.LOCAL }
                    val (downloadStatus, progress) = downloader.getProgress(source?.downloadId)
                    _downloadStatus.emit(Pair(downloadStatus, progress))
                    if (downloadStatus == DownloadManager.STATUS_SUCCESSFUL) {
                        if (source == null) return@launch
                        val path = source.path.replace(".download", "")
                        File(source.path).renameTo(File(path))
                        database.setSourcePath(source.id, path)
                        loadData(item.id)
                    }
                    if (downloadStatus == DownloadManager.STATUS_FAILED) {
                        if (source == null) return@launch
                        downloader.deleteItem(item, source)
                        loadData(item.id)
                    }
                }
                if (item.isDownloading()) {
                    handler.postDelayed(this, 2000L)
                }
            }
        }
        handler.post(downloadProgressRunnable)
    }

    override fun onCleared() {
        super.onCleared()
        handler.removeCallbacksAndMessages(null)
    }
}

sealed interface MovieEvent {
    data object NavigateBack : MovieEvent
    data class DownloadError(val uiText: UiText) : MovieEvent
}
