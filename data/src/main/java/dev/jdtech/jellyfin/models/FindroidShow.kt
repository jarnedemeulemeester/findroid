package dev.jdtech.jellyfin.models

import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.repository.JellyfinRepository
import org.jellyfin.sdk.model.DateTime
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.PlayAccess
import java.util.UUID

data class FindroidShow(
    override val id: UUID,
    override val name: String,
    override val originalTitle: String?,
    override val overview: String,
    override val sources: List<FindroidSource>,
    val seasons: List<FindroidSeason>,
    override val played: Boolean,
    override val favorite: Boolean,
    override val canPlay: Boolean,
    override val canDownload: Boolean,
    override val playbackPositionTicks: Long = 0L,
    override val unplayedItemCount: Int?,
    val genres: List<String>,
    val people: List<FindroidItemPerson>,
    override val runtimeTicks: Long,
    val communityRating: Float?,
    val officialRating: String?,
    val status: String,
    val productionYear: Int?,
    val endDate: DateTime?,
    val trailer: String?,
    override val images: FindroidImages,
    override val chapters: List<FindroidChapter> = emptyList(),
) : FindroidItem

fun BaseItemDto.toFindroidShow(
    jellyfinRepository: JellyfinRepository,
): FindroidShow {
    return FindroidShow(
        id = id,
        name = name.orEmpty(),
        originalTitle = originalTitle,
        overview = overview.orEmpty(),
        played = userData?.played == true,
        favorite = userData?.isFavorite == true,
        canPlay = playAccess != PlayAccess.NONE,
        canDownload = canDownload == true,
        unplayedItemCount = userData?.unplayedItemCount,
        sources = emptyList(),
        seasons = emptyList(),
        genres = genres ?: emptyList(),
        people = people?.map { it.toFindroidPerson(jellyfinRepository) } ?: emptyList(),
        runtimeTicks = runTimeTicks ?: 0,
        communityRating = communityRating,
        officialRating = officialRating,
        status = status ?: "Ended",
        productionYear = productionYear,
        endDate = endDate,
        trailer = remoteTrailers?.getOrNull(0)?.url,
        images = toFindroidImages(jellyfinRepository),
    )
}

fun FindroidShowDto.toFindroidShow(database: ServerDatabaseDao, userId: UUID, baseUrl: String? = null): FindroidShow {
    val userData = database.getUserDataOrCreateNew(id, userId)
    
    // Build image URIs from baseUrl if available
    val images = if (baseUrl != null) {
        val uri = android.net.Uri.parse(baseUrl)
        FindroidImages(
            primary = uri.buildUpon()
                .appendEncodedPath("items/$id/Images/${org.jellyfin.sdk.model.api.ImageType.PRIMARY}")
                .build(),
            backdrop = uri.buildUpon()
                .appendEncodedPath("items/$id/Images/${org.jellyfin.sdk.model.api.ImageType.BACKDROP}/0")
                .build()
        )
    } else {
        FindroidImages()
    }
    
    return FindroidShow(
        id = id,
        name = name,
        originalTitle = originalTitle,
        overview = overview,
        played = userData.played,
        favorite = userData.favorite,
        canPlay = true,
        canDownload = false,
        unplayedItemCount = null,
        sources = emptyList(),
        seasons = emptyList(),
        genres = emptyList(),
        people = emptyList(),
        runtimeTicks = runtimeTicks,
        communityRating = communityRating,
        officialRating = officialRating,
        status = status,
        productionYear = productionYear,
        endDate = endDate,
        trailer = null,
        images = images,
    )
}
