package dev.jdtech.jellyfin.viewmodels

import android.app.Application
import android.net.Uri
import android.os.Build
import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.models.DownloadRequestItem
import dev.jdtech.jellyfin.models.PlayerItem
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.utils.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.BaseItemDto
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
    private val application: Application,
    private val jellyfinRepository: JellyfinRepository
) : ViewModel() {
    private val uiState = MutableStateFlow<UiState>(UiState.Loading)

    sealed class UiState {
        data class Normal(
            val episode: BaseItemDto,
            val runTime: String,
            val dateString: String,
            val played: Boolean,
            val favorite: Boolean,
            val downloaded: Boolean,
            val downloadEpisode: Boolean,
        ) : UiState()

        object Loading : UiState()
        data class Error(val message: String?) : UiState()
    }

    fun onUiState(scope: LifecycleCoroutineScope, collector: (UiState) -> Unit) {
        scope.launch { uiState.collect { collector(it) } }
    }

    var item: BaseItemDto? = null
    var runTime: String = ""
    var dateString: String = ""
    var played: Boolean = false
    var favorite: Boolean = false
    var downloaded: Boolean = false
    var downloadEpisode: Boolean = false
    var playerItems: MutableList<PlayerItem> = mutableListOf()

    lateinit var downloadRequestItem: DownloadRequestItem

    fun loadEpisode(episodeId: UUID) {
        viewModelScope.launch {
            uiState.emit(UiState.Loading)
            try {
                val tempItem = jellyfinRepository.getItem(episodeId)
                item = tempItem
                runTime = "${tempItem.runTimeTicks?.div(600000000)} min"
                dateString = getDateString(tempItem)
                played = tempItem.userData?.played == true
                favorite = tempItem.userData?.isFavorite == true
                downloaded = itemIsDownloaded(episodeId)
                uiState.emit(
                    UiState.Normal(
                        tempItem,
                        runTime,
                        dateString,
                        played,
                        favorite,
                        downloaded,
                        downloadEpisode
                    )
                )
            } catch (e: Exception) {
                uiState.emit(UiState.Error(e.message))
            }
        }
    }

    fun loadEpisode(playerItem: PlayerItem) {
        viewModelScope.launch {
            uiState.emit(UiState.Loading)
            playerItems.add(playerItem)
            item = downloadMetadataToBaseItemDto(playerItem.metadata!!)
            uiState.emit(
                UiState.Normal(
                    item!!,
                    runTime,
                    dateString,
                    played,
                    favorite,
                    downloaded,
                    downloadEpisode
                )
            )
        }
    }

    fun markAsPlayed(itemId: UUID) {
        viewModelScope.launch {
            jellyfinRepository.markAsPlayed(itemId)
        }
        played = true
    }

    fun markAsUnplayed(itemId: UUID) {
        viewModelScope.launch {
            jellyfinRepository.markAsUnplayed(itemId)
        }
        played = false
    }

    fun markAsFavorite(itemId: UUID) {
        viewModelScope.launch {
            jellyfinRepository.markAsFavorite(itemId)
        }
        favorite = true
    }

    fun unmarkAsFavorite(itemId: UUID) {
        viewModelScope.launch {
            jellyfinRepository.unmarkAsFavorite(itemId)
        }
        favorite = false
    }

    fun loadDownloadRequestItem(itemId: UUID) {
        viewModelScope.launch {
            //loadEpisode(itemId)
            val episode = item
            val uri = jellyfinRepository.getStreamUrl(itemId, episode?.mediaSources?.get(0)?.id!!)
            Timber.d(uri)
            val metadata = baseItemDtoToDownloadMetadata(episode)
            downloadRequestItem = DownloadRequestItem(uri, itemId, metadata)
            downloadEpisode = true
            requestDownload(Uri.parse(downloadRequestItem.uri), downloadRequestItem, application)
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
        downloadEpisode = false
        downloaded = true
    }
}