package dev.jdtech.jellyfin.models

import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.repository.JellyfinRepository
import java.util.UUID
import org.jellyfin.sdk.model.DateTime
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemPerson
import org.jellyfin.sdk.model.api.PlayAccess

data class JellyfinMovieItem(
    override val id: UUID,
    override val name: String,
    override val originalTitle: String?,
    override val overview: String,
    override val sources: List<JellyfinSource>,
    override val playedPercentage: Float?,
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
    override val unplayedItemCount: Int? = null,
) : JellyfinItem, JellyfinSources

suspend fun BaseItemDto.toJellyfinMovieItem(
    jellyfinRepository: JellyfinRepository? = null,
    serverDatabase: ServerDatabaseDao? = null
): JellyfinMovieItem {
    val sources = mutableListOf<JellyfinSource>()
    sources.addAll(mediaSources?.map { it.toJellyfinSource(jellyfinRepository, id) } ?: emptyList())
    if (serverDatabase != null) {
        sources.addAll(serverDatabase.getSources(id).map { it.toJellyfinSource() })
    }
    return JellyfinMovieItem(
        id = id,
        name = name.orEmpty(),
        originalTitle = originalTitle,
        overview = overview.orEmpty(),
        sources = sources,
        played = userData?.played ?: false,
        favorite = userData?.isFavorite ?: false,
        playedPercentage = userData?.playedPercentage?.toFloat(),
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
    )
}
