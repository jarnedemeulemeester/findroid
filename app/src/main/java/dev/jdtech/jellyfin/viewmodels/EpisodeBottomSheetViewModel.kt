package dev.jdtech.jellyfin.viewmodels

import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.repository.JellyfinRepository
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.BaseItemDto
import java.text.DateFormat
import java.time.ZoneOffset
import java.util.*
import javax.inject.Inject

@HiltViewModel
class EpisodeBottomSheetViewModel
@Inject
constructor(
    private val jellyfinRepository: JellyfinRepository
) : ViewModel() {

    private val _item = MutableLiveData<BaseItemDto>()
    val item: LiveData<BaseItemDto> = _item

    private val _runTime = MutableLiveData<String>()
    val runTime: LiveData<String> = _runTime

    private val _dateString = MutableLiveData<String>()
    val dateString: LiveData<String> = _dateString

    fun loadEpisode(episodeId: UUID) {
        viewModelScope.launch {
            _item.value = jellyfinRepository.getItem(episodeId)
            _runTime.value = "${_item.value?.runTimeTicks?.div(600000000)} min"
            _dateString.value = getDateString(_item.value!!)
        }
    }

    private fun getDateString(item: BaseItemDto): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val instant = item.premiereDate?.toInstant(ZoneOffset.UTC)
            val date = Date.from(instant)
            DateFormat.getDateInstance(DateFormat.SHORT).format(date)
        } else {
            // TODO: Implement a way to get the year from LocalDateTime in Android < O
            item.premiereDate.toString()
        }
    }
}