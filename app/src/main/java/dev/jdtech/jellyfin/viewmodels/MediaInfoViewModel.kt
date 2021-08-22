package dev.jdtech.jellyfin.viewmodels

import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.models.PlayerItem
import dev.jdtech.jellyfin.repository.JellyfinRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemPerson
import org.jellyfin.sdk.model.api.MediaSourceInfo
import timber.log.Timber
import java.util.*
import javax.inject.Inject

@HiltViewModel
class MediaInfoViewModel
@Inject
constructor(private val jellyfinRepository: JellyfinRepository) : ViewModel() {

    private val _item = MutableLiveData<BaseItemDto>()
    val item: LiveData<BaseItemDto> = _item

    private val _actors = MutableLiveData<List<BaseItemPerson>>()
    val actors: LiveData<List<BaseItemPerson>> = _actors

    private val _director = MutableLiveData<BaseItemPerson>()
    val director: LiveData<BaseItemPerson> = _director

    private val _writers = MutableLiveData<List<BaseItemPerson>>()
    val writers: LiveData<List<BaseItemPerson>> = _writers
    private val _writersString = MutableLiveData<String>()
    val writersString: LiveData<String> = _writersString

    private val _genresString = MutableLiveData<String>()
    val genresString: LiveData<String> = _genresString

    private val _runTime = MutableLiveData<String>()
    val runTime: LiveData<String> = _runTime

    private val _dateString = MutableLiveData<String>()
    val dateString: LiveData<String> = _dateString

    private val _nextUp = MutableLiveData<BaseItemDto>()
    val nextUp: LiveData<BaseItemDto> = _nextUp

    private val _seasons = MutableLiveData<List<BaseItemDto>>()
    val seasons: LiveData<List<BaseItemDto>> = _seasons

    private val _mediaSources = MutableLiveData<List<MediaSourceInfo>>()
    val mediaSources: LiveData<List<MediaSourceInfo>> = _mediaSources

    private val _navigateToPlayer = MutableLiveData<Array<PlayerItem>>()
    val navigateToPlayer: LiveData<Array<PlayerItem>> = _navigateToPlayer

    private val _played = MutableLiveData<Boolean>()
    val played: LiveData<Boolean> = _played

    private val _favorite = MutableLiveData<Boolean>()
    val favorite: LiveData<Boolean> = _favorite

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    var playerItems: MutableList<PlayerItem> = mutableListOf()

    private val _playerItemsError = MutableLiveData<String>()
    val playerItemsError: LiveData<String> = _playerItemsError

    fun loadData(itemId: UUID, itemType: String) {
        _error.value = null
        viewModelScope.launch {
            try {
                _item.value = jellyfinRepository.getItem(itemId)
                _actors.value = getActors(_item.value!!)
                _director.value = getDirector(_item.value!!)
                _writers.value = getWriters(_item.value!!)
                _writersString.value =
                    _writers.value?.joinToString(separator = ", ") { it.name.toString() }
                _genresString.value = _item.value?.genres?.joinToString(separator = ", ")
                _runTime.value = "${_item.value?.runTimeTicks?.div(600000000)} min"
                _dateString.value = getDateString(_item.value!!)
                _played.value = _item.value?.userData?.played
                _favorite.value = _item.value?.userData?.isFavorite
                if (itemType == "Series") {
                    _nextUp.value = getNextUp(itemId)
                    _seasons.value = jellyfinRepository.getSeasons(itemId)
                }
                if (itemType == "Movie") {
                    _mediaSources.value = jellyfinRepository.getMediaSources(itemId)
                }
            } catch (e: Exception) {
                Timber.e(e)
                _error.value = e.message
            }
        }
    }

    private suspend fun getActors(item: BaseItemDto): List<BaseItemPerson>? {
        val actors: List<BaseItemPerson>?
        withContext(Dispatchers.Default) {
            actors = item.people?.filter { it.type == "Actor" }
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

    private suspend fun getWriters(item: BaseItemDto): List<BaseItemPerson>? {
        val writers: List<BaseItemPerson>?
        withContext(Dispatchers.Default) {
            writers = item.people?.filter { it.type == "Writer" }
        }
        return writers
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
            jellyfinRepository.markAsPlayed(itemId)
        }
        _played.value = true
    }

    fun markAsUnplayed(itemId: UUID) {
        viewModelScope.launch {
            jellyfinRepository.markAsUnplayed(itemId)
        }
        _played.value = false
    }

    fun markAsFavorite(itemId: UUID) {
        viewModelScope.launch {
            jellyfinRepository.markAsFavorite(itemId)
        }
        _favorite.value = true
    }

    fun unmarkAsFavorite(itemId: UUID) {
        viewModelScope.launch {
            jellyfinRepository.unmarkAsFavorite(itemId)
        }
        _favorite.value = false
    }

    private fun getDateString(item: BaseItemDto): String {
        val dateString: String = item.productionYear.toString()
        return when (item.status) {
            "Continuing" -> dateString.plus(" - Present")
            "Ended" -> {
                return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    return if (item.productionYear == item.endDate?.year) {
                        dateString
                    } else {
                        dateString.plus(" - ${item.endDate?.year}")
                    }
                } else {
                    // TODO: Implement a way to get the year from LocalDateTime in Android < O
                    dateString
                }

            }
            else -> dateString
        }
    }

    fun preparePlayer() {
        _playerItemsError.value = null
        viewModelScope.launch {
            try {
                createPlayerItems(_item.value!!)
                _navigateToPlayer.value = playerItems.toTypedArray()
            } catch (e: Exception) {
                _playerItemsError.value = e.message
            }
        }
    }

    private suspend fun createPlayerItems(series: BaseItemDto) {
        if (nextUp.value != null) {
            val startEpisode = nextUp.value!!
            val episodes = jellyfinRepository.getEpisodes(startEpisode.seriesId!!, startEpisode.seasonId!!, startIndex = startEpisode.indexNumber?.minus(1))
            for (episode in episodes) {
                val mediaSources = jellyfinRepository.getMediaSources(episode.id)
                playerItems.add(PlayerItem(episode.id, mediaSources[0].id!!))
            }
        } else {
            for (season in seasons.value!!) {
                if (season.indexNumber == 0) continue
                val episodes = jellyfinRepository.getEpisodes(series.id, season.id)
                for (episode in episodes) {
                    val mediaSources = jellyfinRepository.getMediaSources(episode.id)
                    playerItems.add(PlayerItem(episode.id, mediaSources[0].id!!))
                }
            }
        }
    }

    fun navigateToPlayer(mediaSource: MediaSourceInfo) {
        _navigateToPlayer.value = arrayOf(PlayerItem(item.value!!.id, mediaSource.id!!))
    }

    fun doneNavigatingToPlayer() {
        _navigateToPlayer.value = null
    }
}