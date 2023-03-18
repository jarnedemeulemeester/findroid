package dev.jdtech.jellyfin.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.models.Server
import dev.jdtech.jellyfin.models.StorageItem
import dev.jdtech.jellyfin.models.toFindroidEpisode
import dev.jdtech.jellyfin.models.toFindroidMovie
import dev.jdtech.jellyfin.models.toFindroidSeason
import dev.jdtech.jellyfin.models.toFindroidShow
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class StorageViewModel
@Inject
constructor(
    private val database: ServerDatabaseDao
) : ViewModel() {
    private val _serversState = MutableStateFlow<List<Server>>(emptyList())
    val serversState = _serversState.asStateFlow()

    private val _itemsState = MutableStateFlow<List<StorageItem>>(emptyList())
    val itemsState = _itemsState.asStateFlow()

    init {
        loadServers()
    }

    private fun loadServers() {
        viewModelScope.launch {
            try {
                val servers = database.getAllServersSync()
                _serversState.emit(servers)
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    fun loadItems(serverId: String) {
        viewModelScope.launch {
            try {
                val items = mutableListOf<StorageItem>()

                database.getMoviesByServerId(serverId)
                    .map { it.toFindroidMovie(database) }
                    .map { movie ->
                        items.add(
                            StorageItem(
                                item = movie,
                                introTimestamps = database.getIntro(movie.id) != null,
                                trickPlayData = database.getTrickPlayManifest(movie.id) != null,
                                size = File(movie.sources.first().path).length().div(1000000)
                            )
                        )
                    }
                database.getShowsByServerId(serverId)
                    .map { it.toFindroidShow() }
                    .map { show ->
                        items.add(
                            StorageItem(
                                item = show,
                            )
                        )
                        database.getSeasonsByShowId(show.id)
                            .map { it.toFindroidSeason() }
                            .map { season ->
                                items.add(
                                    StorageItem(
                                        item = season,
                                        indent = 1,
                                    )
                                )
                                database.getEpisodesBySeasonId(season.id)
                                    .map { it.toFindroidEpisode(database) }
                                    .map { episode ->
                                        items.add(
                                            StorageItem(
                                                item = episode,
                                                introTimestamps = database.getIntro(episode.id) != null,
                                                trickPlayData = database.getTrickPlayManifest(episode.id) != null,
                                                size = File(episode.sources.first().path).length()
                                                    .div(1000000),
                                                indent = 2,
                                            )
                                        )
                                    }
                            }
                    }

                _itemsState.emit(items)
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }
}
