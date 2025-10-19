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
    val people: List<FindroidItemPerson>,
    val trailer: String?,
    override val unplayedItemCount: Int? = null,
    val missing: Boolean = false,
    override val images: FindroidImages,
    override val chapters: List<FindroidChapter>,
    override val trickplayInfo: Map<String, FindroidTrickplayInfo>?,
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
            played = userData?.played == true,
            favorite = userData?.isFavorite == true,
            canPlay = playAccess != PlayAccess.NONE,
            canDownload = canDownload == true,
            runtimeTicks = runTimeTicks ?: 0,
            playbackPositionTicks = userData?.playbackPositionTicks ?: 0L,
            premiereDate = premiereDate,
            seriesName = seriesName.orEmpty(),
            seriesId = seriesId!!,
            seasonId = seasonId!!,
            communityRating = communityRating,
            people = people?.map { it.toFindroidPerson(jellyfinRepository) } ?: emptyList(),
            trailer = remoteTrailers?.getOrNull(0)?.url,
            missing = locationType == LocationType.VIRTUAL,
            images = toFindroidImages(jellyfinRepository),
            chapters = toFindroidChapters(),
            trickplayInfo = trickplay?.mapValues { it.value[it.value.keys.max()]!!.toFindroidTrickplayInfo() },
        )
    } catch (_: NullPointerException) {
        null
    }
}

fun FindroidEpisodeDto.toFindroidEpisode(database: ServerDatabaseDao, userId: UUID, baseUrl: String? = null): FindroidEpisode {
    val userData = database.getUserDataOrCreateNew(id, userId)
    val sources = database.getSources(id).map { it.toFindroidSource(database) }
    val trickplayInfos = mutableMapOf<String, FindroidTrickplayInfo>()
    for (source in sources) {
        database.getTrickplayInfo(source.id)?.toFindroidTrickplayInfo()?.let {
            trickplayInfos[source.id] = it
        }
    }
    
    // Build image URIs from baseUrl if available
    val images = if (baseUrl != null) {
        val uri = android.net.Uri.parse(baseUrl)
        FindroidImages(
            primary = uri.buildUpon()
                .appendEncodedPath("items/$id/Images/${org.jellyfin.sdk.model.api.ImageType.PRIMARY}")
                .build(),
            showPrimary = uri.buildUpon()
                .appendEncodedPath("items/$seriesId/Images/${org.jellyfin.sdk.model.api.ImageType.PRIMARY}")
                .build()
        )
    } else {
        FindroidImages()
    }
    
    return FindroidEpisode(
        id = id,
        name = name,
        originalTitle = "",
        overview = overview,
        indexNumber = indexNumber,
        indexNumberEnd = indexNumberEnd,
        parentIndexNumber = parentIndexNumber,
        sources = sources,
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
        people = emptyList(),
        trailer = null,
        images = images,
        chapters = chapters ?: emptyList(),
        trickplayInfo = trickplayInfos,
    )
}
