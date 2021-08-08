package dev.jdtech.jellyfin.viewmodels

import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.models.PlayerItem
import dev.jdtech.jellyfin.repository.JellyfinRepository
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.BaseItemDto
import timber.log.Timber
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

    private val _played = MutableLiveData<Boolean>()
    val played: LiveData<Boolean> = _played

    private val _favorite = MutableLiveData<Boolean>()
    val favorite: LiveData<Boolean> = _favorite

    private val _navigateToPlayer = MutableLiveData<Boolean>()
    val navigateToPlayer: LiveData<Boolean> = _navigateToPlayer

    var playerItems: MutableList<PlayerItem> = mutableListOf()

    private val _playerItemsError = MutableLiveData<Boolean>()
    val playerItemsError: LiveData<Boolean> = _playerItemsError

    fun loadEpisode(episodeId: UUID) {
        viewModelScope.launch {
            try {
                val item = jellyfinRepository.getItem(episodeId)
                _item.value = item
                _runTime.value = "${item.runTimeTicks?.div(600000000)} min"
                _dateString.value = getDateString(item)
                _played.value = item.userData?.played
                _favorite.value = item.userData?.isFavorite
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    fun preparePlayer() {
        _playerItemsError.value = false
        viewModelScope.launch {
            try {
                createPlayerItems(_item.value!!)
                _navigateToPlayer.value = true
            } catch (e: Exception) {
                _playerItemsError.value = true
            }
        }
    }

    private suspend fun createPlayerItems(startEpisode: BaseItemDto) {
        val episodes = jellyfinRepository.getEpisodes(
            startEpisode.seriesId!!,
            startEpisode.seasonId!!,
            startIndex = startEpisode.indexNumber?.minus(1)
        )
        for (episode in episodes) {
            val mediaSources = jellyfinRepository.getMediaSources(episode.id)
            playerItems.add(PlayerItem(episode.id, mediaSources[0].id!!))
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
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val instant = item.premiereDate?.toInstant(ZoneOffset.UTC)
            val date = Date.from(instant)
            DateFormat.getDateInstance(DateFormat.SHORT).format(date)
        } else {
            // TODO: Implement a way to get the year from LocalDateTime in Android < O
            item.premiereDate.toString()
        }
    }

    fun doneNavigateToPlayer() {
        _navigateToPlayer.value = false
    }
}