package dev.jdtech.jellyfin.models

import android.net.Uri
import dev.jdtech.jellyfin.repository.JellyfinRepository
import java.util.UUID
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.ImageType

data class FindroidImages(
    val primary: Uri? = null,
    val backdrop: Uri? = null,
    val logo: Uri? = null,
    val showPrimary: Uri? = null,
    val showBackdrop: Uri? = null,
    val showLogo: Uri? = null,
)

fun BaseItemDto.toFindroidImages(jellyfinRepository: JellyfinRepository): FindroidImages {
    val baseUrl = Uri.parse(jellyfinRepository.getBaseUrl())
    val primary =
        imageTags?.get(ImageType.PRIMARY)?.let { tag ->
            baseUrl
                .buildUpon()
                .appendEncodedPath("items/$id/Images/${ImageType.PRIMARY}")
                .appendQueryParameter("tag", tag)
                .build()
        }
    val backdrop =
        backdropImageTags?.firstOrNull()?.let { tag ->
            baseUrl
                .buildUpon()
                .appendEncodedPath("items/$id/Images/${ImageType.BACKDROP}/0")
                .appendQueryParameter("tag", tag)
                .build()
        }
    val logo =
        imageTags?.get(ImageType.LOGO)?.let { tag ->
            baseUrl
                .buildUpon()
                .appendEncodedPath("items/$id/Images/${ImageType.LOGO}")
                .appendQueryParameter("tag", tag)
                .build()
        }
    val showPrimaryOwnerId = parentPrimaryImageItemId ?: seriesId
    val showPrimary =
        showPrimaryOwnerId?.let { ownerId ->
            seriesPrimaryImageTag?.let { tag -> ownerId to tag }
        }?.let { (ownerId, tag) ->
            baseUrl
                .buildUpon()
                .appendEncodedPath("items/$ownerId/Images/${ImageType.PRIMARY}")
                .appendQueryParameter("tag", tag)
                .build()
        }
    val showBackdropOwnerId = parentBackdropItemId ?: seriesId
    val showBackdrop =
        showBackdropOwnerId?.let { ownerId ->
            parentBackdropImageTags?.firstOrNull()?.let { tag -> ownerId to tag }
        }?.let { (ownerId, tag) ->
            baseUrl
                .buildUpon()
                .appendEncodedPath("items/$ownerId/Images/${ImageType.BACKDROP}/0")
                .appendQueryParameter("tag", tag)
                .build()
        }
    val showLogoOwnerId = parentLogoItemId ?: seriesId
    val showLogo =
        showLogoOwnerId?.let { ownerId ->
            parentLogoImageTag?.let { tag -> ownerId to tag }
        }?.let { (ownerId, tag) ->
            baseUrl
                .buildUpon()
                .appendEncodedPath("items/$ownerId/Images/${ImageType.LOGO}")
                .appendQueryParameter("tag", tag)
                .build()
        }

    return FindroidImages(
        primary = primary,
        backdrop = backdrop,
        logo = logo,
        showPrimary = showPrimary,
        showBackdrop = showBackdrop,
        showLogo = showLogo,
    )
}

fun FindroidMovieDto.toLocalFindroidImages(itemId: UUID): FindroidImages {
    return FindroidImages(
        primary = Uri.Builder().appendEncodedPath("images/$itemId/primary").build(),
        backdrop = Uri.Builder().appendEncodedPath("images/$itemId/backdrop").build(),
        logo = Uri.Builder().appendEncodedPath("images/$itemId/logo").build(),
    )
}

fun FindroidShowDto.toLocalFindroidImages(itemId: UUID): FindroidImages {
    return FindroidImages(
        primary = Uri.Builder().appendEncodedPath("images/$itemId/primary").build(),
        backdrop = Uri.Builder().appendEncodedPath("images/$itemId/backdrop").build(),
        logo = Uri.Builder().appendEncodedPath("images/$itemId/logo").build(),
    )
}

fun FindroidSeasonDto.toLocalFindroidImages(itemId: UUID): FindroidImages {
    return FindroidImages(
        primary = Uri.Builder().appendEncodedPath("images/$itemId/primary").build(),
        backdrop = Uri.Builder().appendEncodedPath("images/$itemId/backdrop").build(),
        logo = Uri.Builder().appendEncodedPath("images/$itemId/logo").build(),
        showPrimary = Uri.Builder().appendEncodedPath("images/$seriesId/primary").build(),
        showBackdrop = Uri.Builder().appendEncodedPath("images/$seriesId/backdrop").build(),
        showLogo = Uri.Builder().appendEncodedPath("images/$seriesId/logo").build(),
    )
}

fun FindroidEpisodeDto.toLocalFindroidImages(itemId: UUID): FindroidImages {
    return FindroidImages(
        primary = Uri.Builder().appendEncodedPath("images/$itemId/primary").build(),
        backdrop = Uri.Builder().appendEncodedPath("images/$itemId/backdrop").build(),
        logo = Uri.Builder().appendEncodedPath("images/$itemId/logo").build(),
        showPrimary = Uri.Builder().appendEncodedPath("images/$seriesId/primary").build(),
        showBackdrop = Uri.Builder().appendEncodedPath("images/$seriesId/backdrop").build(),
        showLogo = Uri.Builder().appendEncodedPath("images/$seriesId/logo").build(),
    )
}
