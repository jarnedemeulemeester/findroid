package dev.jdtech.jellyfin.models

import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.repository.JellyfinRepository
import org.jellyfin.sdk.model.DateTime
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemPerson
import org.jellyfin.sdk.model.api.PlayAccess
import java.util.UUID

data class FindroidMovie(
    override val id: UUID,
    override val name: String,
    override val originalTitle: String?,
    override val overview: String,
    override val sources: List<FindroidSource>,
    override val played: Boolean,
    override val favorite: Boolean,
    override val canPlay: Boolean,
    override val canDownload: Boolean,
    override val runtimeTicks: Long,
    override val playbackPositionTicks: Long,
    val premiereDate: DateTime?,
    val people: List<BaseItemPerson>,
    val genres: List<String>,
    val communityRating: Float?,
    val officialRating: String?,
    val status: String,
    val productionYear: Int?,
    val endDate: DateTime?,
    val trailer: String?,
    override val unplayedItemCount: Int? = null,
) : FindroidItem, FindroidSources

suspend fun BaseItemDto.toFindroidMovie(
    jellyfinRepository: JellyfinRepository,
    serverDatabase: ServerDatabaseDao? = null,
): FindroidMovie {
    val sources = mutableListOf<FindroidSource>()
    sources.addAll(mediaSources?.map { it.toFindroidSource(jellyfinRepository, id) } ?: emptyList())
    if (serverDatabase != null) {
        sources.addAll(serverDatabase.getSources(id).map { it.toFindroidSource(serverDatabase) })
    }
    return FindroidMovie(
        id = id,
        name = name.orEmpty(),
        originalTitle = originalTitle,
        overview = overview.orEmpty(),
        sources = sources,
        played = userData?.played ?: false,
        favorite = userData?.isFavorite ?: false,
        canPlay = playAccess != PlayAccess.NONE,
        canDownload = canDownload ?: false,
        runtimeTicks = runTimeTicks ?: 0,
        playbackPositionTicks = userData?.playbackPositionTicks ?: 0,
        premiereDate = premiereDate,
        communityRating = communityRating,
        genres = genres ?: emptyList(),
        people = people ?: emptyList(),
        officialRating = officialRating,
        status = status ?: "Ended",
        productionYear = productionYear,
        endDate = endDate,
        trailer = remoteTrailers?.getOrNull(0)?.url,
    )
}

fun FindroidMovieDto.toFindroidMovie(database: ServerDatabaseDao, userId: UUID): FindroidMovie {
    val userData = database.getUserDataOrCreateNew(id, userId)
    return FindroidMovie(
        id = id,
        name = name,
        originalTitle = originalTitle,
        overview = overview,
        played = userData.played,
        favorite = userData.favorite,
        runtimeTicks = runtimeTicks,
        playbackPositionTicks = userData.playbackPositionTicks,
        premiereDate = premiereDate,
        genres = emptyList(),
        people = emptyList(),
        communityRating = communityRating,
        officialRating = officialRating,
        status = status,
        productionYear = productionYear,
        endDate = endDate,
        canDownload = false,
        canPlay = true,
        sources = database.getSources(id).map { it.toFindroidSource(database) },
        trailer = null,
    )
}
