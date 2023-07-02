package dev.jdtech.jellyfin.models

import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.repository.JellyfinRepository
import org.jellyfin.sdk.model.DateTime
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.LocationType
import org.jellyfin.sdk.model.api.PlayAccess
import java.util.UUID

data class FindroidEpisode(
    override val id: UUID,
    override val name: String,
    override val originalTitle: String?,
    override val overview: String,
    val indexNumber: Int,
    val indexNumberEnd: Int?,
    val parentIndexNumber: Int,
    override val sources: List<FindroidSource>,
    override val played: Boolean,
    override val favorite: Boolean,
    override val canPlay: Boolean,
    override val canDownload: Boolean,
    override val runtimeTicks: Long,
    override val playbackPositionTicks: Long,
    val premiereDate: DateTime?,
    val seriesName: String,
    val seriesId: UUID,
    val seasonId: UUID,
    val communityRating: Float?,
    override val unplayedItemCount: Int? = null,
    val missing: Boolean = false,
) : FindroidItem, FindroidSources

suspend fun BaseItemDto.toFindroidEpisode(
    jellyfinRepository: JellyfinRepository,
    database: ServerDatabaseDao? = null,
): FindroidEpisode? {
    val sources = mutableListOf<FindroidSource>()
    sources.addAll(mediaSources?.map { it.toFindroidSource(jellyfinRepository, id) } ?: emptyList())
    if (database != null) {
        sources.addAll(database.getSources(id).map { it.toFindroidSource(database) })
    }
    return try {
        FindroidEpisode(
            id = id,
            name = name.orEmpty(),
            originalTitle = originalTitle,
            overview = overview.orEmpty(),
            indexNumber = indexNumber ?: 0,
            indexNumberEnd = indexNumberEnd,
            parentIndexNumber = parentIndexNumber ?: 0,
            sources = sources,
            played = userData?.played ?: false,
            favorite = userData?.isFavorite ?: false,
            canPlay = playAccess != PlayAccess.NONE,
            canDownload = canDownload ?: false,
            runtimeTicks = runTimeTicks ?: 0,
            playbackPositionTicks = userData?.playbackPositionTicks ?: 0L,
            premiereDate = premiereDate,
            seriesName = seriesName.orEmpty(),
            seriesId = seriesId!!,
            seasonId = seasonId!!,
            communityRating = communityRating,
            missing = locationType == LocationType.VIRTUAL,
        )
    } catch (_: NullPointerException) {
        null
    }
}

fun FindroidEpisodeDto.toFindroidEpisode(database: ServerDatabaseDao, userId: UUID): FindroidEpisode {
    val userData = database.getUserDataOrCreateNew(id, userId)
    return FindroidEpisode(
        id = id,
        name = name,
        originalTitle = "",
        overview = overview,
        indexNumber = indexNumber,
        indexNumberEnd = indexNumberEnd,
        parentIndexNumber = parentIndexNumber,
        sources = database.getSources(id).map { it.toFindroidSource(database) },
        played = userData.played,
        favorite = userData.favorite,
        canPlay = true,
        canDownload = false,
        runtimeTicks = runtimeTicks,
        playbackPositionTicks = userData.playbackPositionTicks,
        premiereDate = premiereDate,
        seriesName = seriesName,
        seriesId = seriesId,
        seasonId = seasonId,
        communityRating = communityRating,
    )
}
