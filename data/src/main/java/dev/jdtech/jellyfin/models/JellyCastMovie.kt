package dev.jdtech.jellyfin.models

import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.repository.JellyfinRepository
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.PlayAccess
import java.time.LocalDateTime
import java.util.UUID

data class JellyCastMovie(
    override val id: UUID,
    override val name: String,
    override val originalTitle: String?,
    override val overview: String,
    override val sources: List<JellyCastSource>,
    override val played: Boolean,
    override val favorite: Boolean,
    override val canPlay: Boolean,
    override val canDownload: Boolean,
    override val runtimeTicks: Long,
    override val playbackPositionTicks: Long,
    val premiereDate: LocalDateTime?,
    val people: List<JellyCastItemPerson>,
    val genres: List<String>,
    val communityRating: Float?,
    val officialRating: String?,
    val status: String,
    val productionYear: Int?,
    val endDate: LocalDateTime?,
    val trailer: String?,
    override val unplayedItemCount: Int? = null,
    override val images: JellyCastImages,
    override val chapters: List<JellyCastChapter>,
    override val trickplayInfo: Map<String, JellyCastTrickplayInfo>?,
) : JellyCastItem, JellyCastSources

suspend fun BaseItemDto.toJellyCastMovie(
    jellyfinRepository: JellyfinRepository,
    serverDatabase: ServerDatabaseDao? = null,
): JellyCastMovie {
    val sources = mutableListOf<JellyCastSource>()
    sources.addAll(mediaSources?.map { it.toJellyCastSource(jellyfinRepository, id) } ?: emptyList())
    if (serverDatabase != null) {
        sources.addAll(serverDatabase.getSources(id).map { it.toJellyCastSource(serverDatabase) })
    }
    return JellyCastMovie(
        id = id,
        name = name.orEmpty(),
        originalTitle = originalTitle,
        overview = overview.orEmpty(),
        sources = sources,
        played = userData?.played == true,
        favorite = userData?.isFavorite == true,
        canPlay = playAccess != PlayAccess.NONE,
        canDownload = canDownload == true,
        runtimeTicks = runTimeTicks ?: 0,
        playbackPositionTicks = userData?.playbackPositionTicks ?: 0,
        premiereDate = premiereDate,
        communityRating = communityRating,
        genres = genres ?: emptyList(),
        people = people?.map { it.toJellyCastPerson(jellyfinRepository) } ?: emptyList(),
        officialRating = officialRating,
        status = status ?: "Ended",
        productionYear = productionYear,
        endDate = endDate,
        trailer = remoteTrailers?.getOrNull(0)?.url,
        images = toJellyCastImages(jellyfinRepository),
        chapters = toJellyCastChapters(),
        trickplayInfo = trickplay?.mapValues { it.value[it.value.keys.max()]!!.toJellyCastTrickplayInfo() },
    )
}

fun JellyCastMovieDto.toJellyCastMovie(database: ServerDatabaseDao, userId: UUID, baseUrl: String? = null): JellyCastMovie {
    val userData = database.getUserDataOrCreateNew(id, userId)
    val sources = database.getSources(id).map { it.toJellyCastSource(database) }
    val trickplayInfos = mutableMapOf<String, JellyCastTrickplayInfo>()
    for (source in sources) {
        database.getTrickplayInfo(source.id)?.toJellyCastTrickplayInfo()?.let {
            trickplayInfos[source.id] = it
        }
    }
    
    // Build image URIs from baseUrl if available
    val images = if (baseUrl != null) {
        val uri = android.net.Uri.parse(baseUrl)
        JellyCastImages(
            primary = uri.buildUpon()
                .appendEncodedPath("items/$id/Images/${org.jellyfin.sdk.model.api.ImageType.PRIMARY}")
                .build(),
            backdrop = uri.buildUpon()
                .appendEncodedPath("items/$id/Images/${org.jellyfin.sdk.model.api.ImageType.BACKDROP}/0")
                .build()
        )
    } else {
        JellyCastImages()
    }
    
    return JellyCastMovie(
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
        sources = database.getSources(id).map { it.toJellyCastSource(database) },
        trailer = null,
        images = images,
        chapters = chapters ?: emptyList(),
        trickplayInfo = trickplayInfos,
    )
}
