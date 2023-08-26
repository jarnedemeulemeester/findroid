package dev.jdtech.jellyfin.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidSeason
import dev.jdtech.jellyfin.models.FindroidShow
import dev.jdtech.jellyfin.repository.JellyfinRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.model.api.BaseItemPerson
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ShowViewModel
@Inject
constructor(
    private val jellyfinRepository: JellyfinRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _navigateBack = MutableSharedFlow<Boolean>()
    val navigateBack = _navigateBack.asSharedFlow()

    sealed class UiState {
        data class Normal(
            val item: FindroidShow,
            val actors: List<BaseItemPerson>,
            val director: BaseItemPerson?,
            val writers: List<BaseItemPerson>,
            val writersString: String,
            val genresString: String,
            val runTime: String,
            val dateString: String,
            val nextUp: FindroidEpisode?,
            val seasons: List<FindroidSeason>,
        ) : UiState()

        data object Loading : UiState()
        data class Error(val error: Exception) : UiState()
    }

    lateinit var item: FindroidShow
    private var played: Boolean = false
    private var favorite: Boolean = false
    private var actors: List<BaseItemPerson> = emptyList()
    private var director: BaseItemPerson? = null
    private var writers: List<BaseItemPerson> = emptyList()
    private var writersString: String = ""
    private var genresString: String = ""
    private var runTime: String = ""
    private var dateString: String = ""
    var nextUp: FindroidEpisode? = null
    var seasons: List<FindroidSeason> = emptyList()

    fun loadData(itemId: UUID, offline: Boolean) {
        viewModelScope.launch {
            _uiState.emit(UiState.Loading)
            try {
                item = jellyfinRepository.getShow(itemId)
                played = item.played
                favorite = item.favorite
                actors = getActors(item)
                director = getDirector(item)
                writers = getWriters(item)
                writersString = writers.joinToString(separator = ", ") { it.name.toString() }
                genresString = item.genres.joinToString(separator = ", ")
                runTime = "${item.runtimeTicks.div(600000000)} min"
                dateString = getDateString(item)
                nextUp = getNextUp(itemId)
                seasons = jellyfinRepository.getSeasons(itemId, offline)
                _uiState.emit(
                    UiState.Normal(
                        item,
                        actors,
                        director,
                        writers,
                        writersString,
                        genresString,
                        runTime,
                        dateString,
                        nextUp,
                        seasons,
                    ),
                )
            } catch (_: NullPointerException) {
                // Navigate back because item does not exist (probably because it's been deleted)
                _navigateBack.emit(true)
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

    private suspend fun getNextUp(seriesId: UUID): FindroidEpisode? {
        val nextUpItems = jellyfinRepository.getNextUp(seriesId)
        return nextUpItems.getOrNull(0)
    }

    fun togglePlayed(): Boolean {
        when (played) {
            false -> {
                played = true
                viewModelScope.launch {
                    try {
                        jellyfinRepository.markAsPlayed(item.id)
                    } catch (_: Exception) {}
                }
            }
            true -> {
                played = false
                viewModelScope.launch {
                    try {
                        jellyfinRepository.markAsUnplayed(item.id)
                    } catch (_: Exception) {}
                }
            }
        }
        return played
    }

    fun toggleFavorite(): Boolean {
        when (favorite) {
            false -> {
                favorite = true
                viewModelScope.launch {
                    try {
                        jellyfinRepository.markAsFavorite(item.id)
                    } catch (_: Exception) {}
                }
            }
            true -> {
                favorite = false
                viewModelScope.launch {
                    try {
                        jellyfinRepository.unmarkAsFavorite(item.id)
                    } catch (_: Exception) {}
                }
            }
        }
        return favorite
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
}
