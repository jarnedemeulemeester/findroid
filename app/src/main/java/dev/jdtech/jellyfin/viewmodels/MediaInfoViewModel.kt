package dev.jdtech.jellyfin.viewmodels

import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.repository.JellyfinRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemPerson
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

    fun loadData(itemId: UUID) {
        viewModelScope.launch {
            _item.value = jellyfinRepository.getItem(itemId)
            _actors.value = getActors(_item.value!!)
            _director.value = getDirector(_item.value!!)
            _writers.value = getWriters(_item.value!!)
            _writersString.value =
                _writers.value?.joinToString(separator = ", ") { it.name.toString() }
            _genresString.value = _item.value?.genres?.joinToString(separator = ", ")
            _runTime.value = "${_item.value?.runTimeTicks?.div(600000000)} min"
            _dateString.value = getDateString(_item.value!!)
            if (_item.value!!.type == "Series") {
                _nextUp.value = getNextUp(itemId)
                _seasons.value = jellyfinRepository.getSeasons(itemId)
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