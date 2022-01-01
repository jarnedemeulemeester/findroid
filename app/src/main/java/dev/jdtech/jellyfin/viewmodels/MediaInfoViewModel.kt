package dev.jdtech.jellyfin.viewmodels

import android.app.Application
import android.net.Uri
import android.os.Build
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.models.DownloadRequestItem
import dev.jdtech.jellyfin.models.PlayerItem
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.utils.baseItemDtoToDownloadMetadata
import dev.jdtech.jellyfin.utils.deleteDownloadedEpisode
import dev.jdtech.jellyfin.utils.downloadMetadataToBaseItemDto
import dev.jdtech.jellyfin.utils.itemIsDownloaded
import dev.jdtech.jellyfin.utils.requestDownload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemPerson
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class MediaInfoViewModel
@Inject
constructor(
    private val application: Application,
    private val jellyfinRepository: JellyfinRepository
) : ViewModel() {
    private val uiState = MutableStateFlow<UiState>(UiState.Loading)

    sealed class UiState {
        data class Normal(
            val item: BaseItemDto,
            val actors: List<BaseItemPerson>,
            val director: BaseItemPerson?,
            val writers: List<BaseItemPerson>,
            val writersString: String,
            val genresString: String,
            val runTime: String,
            val dateString: String,
            val nextUp: BaseItemDto?,
            val seasons: List<BaseItemDto>,
            val played: Boolean,
            val favorite: Boolean,
            val downloaded: Boolean,
        ) : UiState()

        object Loading : UiState()
        data class Error(val message: String?) : UiState()
    }

    fun onUiState(scope: LifecycleCoroutineScope, collector: (UiState) -> Unit) {
        scope.launch { uiState.collect { collector(it) } }
    }

    var item: BaseItemDto? = null
    private var actors: List<BaseItemPerson> = emptyList()
    private var director: BaseItemPerson? = null
    private var writers: List<BaseItemPerson> = emptyList()
    private var writersString: String = ""
    private var genresString: String = ""
    private var runTime: String = ""
    private var dateString: String = ""
    var nextUp: BaseItemDto? = null
    var seasons: List<BaseItemDto> = emptyList()
    var played: Boolean = false
    var favorite: Boolean = false
    private var downloaded: Boolean = false
    private var downloadMedia: Boolean = false

    private lateinit var downloadRequestItem: DownloadRequestItem

    lateinit var playerItem: PlayerItem

    fun loadData(itemId: UUID, itemType: String) {
        viewModelScope.launch {
            uiState.emit(UiState.Loading)
            try {
                val tempItem = jellyfinRepository.getItem(itemId)
                item = tempItem
                actors = getActors(tempItem)
                director = getDirector(tempItem)
                writers = getWriters(tempItem)
                writersString = writers.joinToString(separator = ", ") { it.name.toString() }
                genresString = tempItem.genres?.joinToString(separator = ", ") ?: ""
                runTime = "${tempItem.runTimeTicks?.div(600000000)} min"
                dateString = getDateString(tempItem)
                played = tempItem.userData?.played ?: false
                favorite = tempItem.userData?.isFavorite ?: false
                downloaded = itemIsDownloaded(itemId)
                if (itemType == "Series") {
                    nextUp = getNextUp(itemId)
                    seasons = jellyfinRepository.getSeasons(itemId)
                }
                uiState.emit(
                    UiState.Normal(
                        tempItem,
                        actors,
                        director,
                        writers,
                        writersString,
                        genresString,
                        runTime,
                        dateString,
                        nextUp,
                        seasons,
                        played,
                        favorite,
                        downloaded
                    )
                )
            } catch (e: Exception) {
                Timber.d(e)
                Timber.d(itemId.toString())
                uiState.emit(UiState.Error(e.message))
            }
        }
    }

    fun loadData(pItem: PlayerItem) {
        viewModelScope.launch {
            playerItem = pItem
            val tempItem = downloadMetadataToBaseItemDto(playerItem.metadata!!)
            item = tempItem
            actors = getActors(tempItem)
            director = getDirector(tempItem)
            writers = getWriters(tempItem)
            writersString = writers.joinToString(separator = ", ") { it.name.toString() }
            genresString = tempItem.genres?.joinToString(separator = ", ") ?: ""
            runTime = ""
            dateString = ""
            played = tempItem.userData?.played ?: false
            favorite = tempItem.userData?.isFavorite ?: false
            uiState.emit(
                UiState.Normal(
                    tempItem,
                    actors,
                    director,
                    writers,
                    writersString,
                    genresString,
                    runTime,
                    dateString,
                    nextUp,
                    seasons,
                    played,
                    favorite,
                    downloaded
                )
            )
        }
    }

    private suspend fun getActors(item: BaseItemDto): List<BaseItemPerson> {
        val actors: List<BaseItemPerson>
        withContext(Dispatchers.Default) {
            actors = item.people?.filter { it.type == "Actor" } ?: emptyList()
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

    private suspend fun getWriters(item: BaseItemDto): List<BaseItemPerson> {
        val writers: List<BaseItemPerson>
        withContext(Dispatchers.Default) {
            writers = item.people?.filter { it.type == "Writer" } ?: emptyList()
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

    fun loadDownloadRequestItem(itemId: UUID) {
        viewModelScope.launch {
            val downloadItem = item
            val uri =
                jellyfinRepository.getStreamUrl(itemId, downloadItem?.mediaSources?.get(0)?.id!!)
            val metadata = baseItemDtoToDownloadMetadata(downloadItem)
            downloadRequestItem = DownloadRequestItem(uri, itemId, metadata)
            downloadMedia = true
            requestDownload(Uri.parse(downloadRequestItem.uri), downloadRequestItem, application)
        }
    }

    fun deleteItem() {
        deleteDownloadedEpisode(playerItem.mediaSourceUri)
    }
}