package dev.jdtech.jellyfin.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidMediaStream
import dev.jdtech.jellyfin.models.FindroidSeason
import dev.jdtech.jellyfin.models.FindroidShow
import dev.jdtech.jellyfin.repository.JellyfinRepository
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.model.api.BaseItemPerson
import org.jellyfin.sdk.model.api.MediaStreamType
import timber.log.Timber

@HiltViewModel
class ShowViewModel
@Inject
constructor(
    private val jellyfinRepository: JellyfinRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    sealed class UiState {
        data class Normal(
            val item: FindroidShow,
            val actors: List<BaseItemPerson>,
            val director: BaseItemPerson?,
            val writers: List<BaseItemPerson>,
            val writersString: String,
            val genresString: String,
            val videoString: String,
            val audioString: String,
            val subtitleString: String,
            val runTime: String,
            val dateString: String,
            val nextUp: FindroidEpisode?,
            val seasons: List<FindroidSeason>,
        ) : UiState()

        object Loading : UiState()
        data class Error(val error: Exception) : UiState()
    }

    lateinit var item: FindroidShow
    private var actors: List<BaseItemPerson> = emptyList()
    private var director: BaseItemPerson? = null
    private var writers: List<BaseItemPerson> = emptyList()
    private var writersString: String = ""
    private var genresString: String = ""
    private var videoString: String = ""
    private var audioString: String = ""
    private var subtitleString: String = ""
    private var runTime: String = ""
    private var dateString: String = ""
    var nextUp: FindroidEpisode? = null
    var seasons: List<FindroidSeason> = emptyList()

    fun loadData(itemId: UUID) {
        viewModelScope.launch {
            _uiState.emit(UiState.Loading)
            try {
                item = jellyfinRepository.getShow(itemId)
                actors = getActors(item)
                director = getDirector(item)
                writers = getWriters(item)
                writersString = writers.joinToString(separator = ", ") { it.name.toString() }
                genresString = item.genres.joinToString(separator = ", ")
                videoString = getMediaString(item, MediaStreamType.VIDEO)
                audioString = getMediaString(item, MediaStreamType.AUDIO)
                subtitleString = getMediaString(item, MediaStreamType.SUBTITLE)
                runTime = "${item.runtimeTicks.div(600000000)} min"
                dateString = getDateString(item)
                nextUp = getNextUp(itemId)
                seasons = jellyfinRepository.getSeasons(itemId)
                _uiState.emit(
                    UiState.Normal(
                        item,
                        actors,
                        director,
                        writers,
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

    private suspend fun getActors(item: FindroidShow): List<BaseItemPerson> {
        val actors: List<BaseItemPerson>
        withContext(Dispatchers.Default) {
            actors = item.people.filter { it.type == "Actor" }
        }
        return actors
    }

    private suspend fun getDirector(item: FindroidShow): BaseItemPerson? {
        val director: BaseItemPerson?
        withContext(Dispatchers.Default) {
            director = item.people.firstOrNull { it.type == "Director" }
        }
        return director
    }

    private suspend fun getWriters(item: FindroidShow): List<BaseItemPerson> {
        val writers: List<BaseItemPerson>
        withContext(Dispatchers.Default) {
            writers = item.people.filter { it.type == "Writer" }
        }
        return writers
    }

    private suspend fun getMediaString(item: FindroidShow, type: MediaStreamType): String {
        val streams: List<FindroidMediaStream>
        withContext(Dispatchers.Default) {
            streams = item.sources.getOrNull(0)?.mediaStreams?.filter { it.type == type } ?: emptyList()
        }
        return streams.map { it.displayTitle }.joinToString(separator = ", ")
    }

    private suspend fun getNextUp(seriesId: UUID): FindroidEpisode? {
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
                loadData(item.id)
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
                loadData(item.id)
            } catch (e: ApiClientException) {
                Timber.d(e)
            }
        }
    }

    private fun getDateString(item: FindroidShow): String {
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
