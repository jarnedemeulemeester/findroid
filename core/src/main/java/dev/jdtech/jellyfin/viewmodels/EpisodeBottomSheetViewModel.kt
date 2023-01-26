package dev.jdtech.jellyfin.viewmodels

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.database.DownloadDatabaseDao
import dev.jdtech.jellyfin.models.PlayerItem
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.utils.canRetryDownload
import dev.jdtech.jellyfin.utils.deleteDownloadedEpisode
import dev.jdtech.jellyfin.utils.downloadMetadataToBaseItemDto
import dev.jdtech.jellyfin.utils.isItemAvailable
import dev.jdtech.jellyfin.utils.isItemDownloaded
import dev.jdtech.jellyfin.utils.requestDownload
import java.text.DateFormat
import java.time.ZoneOffset
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.model.DateTime
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.PlayAccess
import timber.log.Timber

@HiltViewModel
class EpisodeBottomSheetViewModel
@Inject
constructor(
    private val application: Application,
    private val jellyfinRepository: JellyfinRepository,
    private val downloadDatabase: DownloadDatabaseDao
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    sealed class UiState {
        data class Normal(
            val episode: BaseItemDto,
            val runTime: String,
            val dateString: String,
            val played: Boolean,
            val favorite: Boolean,
            val canPlay: Boolean,
            val canDownload: Boolean,
            val downloaded: Boolean,
            val available: Boolean,
            val canRetry: Boolean
        ) : UiState()

        object Loading : UiState()
        data class Error(val error: Exception) : UiState()
    }

    var item: BaseItemDto? = null
    private var runTime: String = ""
    private var dateString: String = ""
    var played: Boolean = false
    var favorite: Boolean = false
    private var canPlay = true
    private var canDownload = false
    private var downloaded: Boolean = false
    private var available: Boolean = true
    var canRetry: Boolean = false
    var playerItems: MutableList<PlayerItem> = mutableListOf()

    fun loadEpisode(episodeId: UUID) {
        viewModelScope.launch {
            _uiState.emit(UiState.Loading)
            try {
                val tempItem = jellyfinRepository.getItem(episodeId)
                item = tempItem
                runTime = "${tempItem.runTimeTicks?.div(600000000)} min"
                dateString = getDateString(tempItem.premiereDate)
                played = tempItem.userData?.played == true
                favorite = tempItem.userData?.isFavorite == true
                canPlay = tempItem.playAccess != PlayAccess.NONE
                canDownload = tempItem.canDownload == true
                downloaded = isItemDownloaded(downloadDatabase, episodeId)
                _uiState.emit(
                    UiState.Normal(
                        tempItem,
                        runTime,
                        dateString,
                        played,
                        favorite,
                        canPlay,
                        canDownload,
                        downloaded,
                        available,
                        canRetry,
                    )
                )
            } catch (e: Exception) {
                _uiState.emit(UiState.Error(e))
            }
        }
    }

    fun loadEpisode(playerItem: PlayerItem) {
        viewModelScope.launch {
            _uiState.emit(UiState.Loading)
            playerItems.add(playerItem)
            item = downloadMetadataToBaseItemDto(playerItem.item!!)
            available = isItemAvailable(playerItem.itemId)
            canRetry = canRetryDownload(playerItem.itemId, downloadDatabase, application)
            _uiState.emit(
                UiState.Normal(
                    item!!,
                    runTime,
                    dateString,
                    played,
                    favorite,
                    canPlay,
                    canDownload,
                    downloaded,
                    available,
                    canRetry,
                )
            )
        }
    }

    fun markAsPlayed(itemId: UUID) {
        viewModelScope.launch {
            try {
                jellyfinRepository.markAsPlayed(itemId)
            } catch (e: ApiClientException) {
                Timber.d(e)
            }
        }
        played = true
    }

    fun markAsUnplayed(itemId: UUID) {
        viewModelScope.launch {
            try {
                jellyfinRepository.markAsUnplayed(itemId)
            } catch (e: ApiClientException) {
                Timber.d(e)
            }
        }
        played = false
    }

    fun markAsFavorite(itemId: UUID) {
        viewModelScope.launch {
            try {
                jellyfinRepository.markAsFavorite(itemId)
            } catch (e: ApiClientException) {
                Timber.d(e)
            }
        }
        favorite = true
    }

    fun unmarkAsFavorite(itemId: UUID) {
        viewModelScope.launch {
            try {
                jellyfinRepository.unmarkAsFavorite(itemId)
            } catch (e: ApiClientException) {
                Timber.d(e)
            }
        }
        favorite = false
    }

    fun download() {
        viewModelScope.launch {
            requestDownload(
                jellyfinRepository,
                downloadDatabase,
                application,
                item!!.id
            )
        }
    }

    fun deleteEpisode() {
        deleteDownloadedEpisode(downloadDatabase, playerItems[0].itemId)
    }

    private fun getDateString(datetime: DateTime?): String {
        if (datetime == null) return ""
        val instant = datetime.toInstant(ZoneOffset.UTC)
        val date = Date.from(instant)
        return DateFormat.getDateInstance(DateFormat.SHORT).format(date)
    }
}
