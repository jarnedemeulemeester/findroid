package dev.jdtech.jellyfin.viewmodels

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dev.jdtech.jellyfin.api.JellyfinApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemPerson
import java.util.*

class MediaInfoViewModel(application: Application, itemId: UUID) : AndroidViewModel(application) {
    private val jellyfinApi = JellyfinApi.getInstance(application, "")

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

    init {
        viewModelScope.launch {
            _item.value = getItemDetails(itemId)
            _actors.value = getActors(_item.value!!)
            _director.value = getDirector(_item.value!!)
            _writers.value = getWriters(_item.value!!)
            _writersString.value =
                _writers.value?.joinToString(separator = ", ") { it.name.toString() }
            _genresString.value = _item.value?.genres?.joinToString(separator = ", ")
            _runTime.value = "${_item.value?.runTimeTicks?.div(600000000)} min"
            _dateString.value = getDateString(_item.value!!)
            _item.value!!.status?.let { Log.i("MediaInfoViewModel", it) }
            if (_item.value!!.type == "Series") {
                _nextUp.value = getNextUp(itemId)
                _seasons.value = getSeasons(itemId)
            }
        }
    }

    private suspend fun getItemDetails(itemId: UUID): BaseItemDto {
        val item: BaseItemDto
        withContext(Dispatchers.IO) {
            item = jellyfinApi.userLibraryApi.getItem(jellyfinApi.userId!!, itemId).content
        }
        return item
    }

    private suspend fun getSeasons(itemId: UUID): List<BaseItemDto>? {
        val seasons: List<BaseItemDto>?
        withContext(Dispatchers.IO) {
            seasons = jellyfinApi.showsApi.getSeasons(itemId, jellyfinApi.userId!!).content.items
        }
        return seasons
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
        val nextUpItems: List<BaseItemDto>?
        withContext(Dispatchers.IO) {
            nextUpItems = jellyfinApi.showsApi.getNextUp(
                jellyfinApi.userId!!,
                seriesId = seriesId.toString()
            ).content.items
        }
        if (nextUpItems != null) {
            return if (nextUpItems.isNotEmpty()) {
                nextUpItems[0]
            } else {
                null
            }
        }
        return null
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
}