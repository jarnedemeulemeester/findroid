package dev.jdtech.jellyfin.viewmodels

import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.models.DownloadMetadata
import dev.jdtech.jellyfin.models.DownloadRequestItem
import dev.jdtech.jellyfin.models.PlayerItem
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.utils.baseItemDtoToDownloadMetadata
import dev.jdtech.jellyfin.utils.deleteDownloadedEpisode
import dev.jdtech.jellyfin.utils.downloadMetadataToBaseItemDto
import dev.jdtech.jellyfin.utils.itemIsDownloaded
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.ItemFields
import org.jellyfin.sdk.model.api.LocationType
import timber.log.Timber
import java.text.DateFormat
import java.time.ZoneOffset
import java.util.Date
import java.util.UUID
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

    private val _downloaded = MutableLiveData<Boolean>()
    val downloaded: LiveData<Boolean> = _downloaded

    private val _downloadEpisode = MutableLiveData<Boolean>()
    val downloadEpisode: LiveData<Boolean> = _downloadEpisode

    var playerItems: MutableList<PlayerItem> = mutableListOf()

    lateinit var downloadRequestItem: DownloadRequestItem

    fun loadEpisode(episodeId: UUID) {
        viewModelScope.launch {
            try {
                _downloaded.value = itemIsDownloaded(episodeId)
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

    fun loadEpisode(playerItem : PlayerItem){
        playerItems.add(playerItem)
        _item.value = downloadMetadataToBaseItemDto(playerItem.metadata!!)
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

    fun loadDownloadRequestItem(itemId: UUID) {
        viewModelScope.launch {
            loadEpisode(itemId)
            val episode = _item.value
            val uri = jellyfinRepository.getStreamUrl(itemId, episode?.mediaSources?.get(0)?.id!!)
            val metadata = baseItemDtoToDownloadMetadata(episode)
            downloadRequestItem = DownloadRequestItem(uri, itemId, metadata)
            _downloadEpisode.value = true
        }
    }

    fun deleteEpisode() {
        deleteDownloadedEpisode(playerItems[0].mediaSourceUri)
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

    fun doneDownloadEpisode() {
        _downloadEpisode.value = false
        _downloaded.value = true
    }
}