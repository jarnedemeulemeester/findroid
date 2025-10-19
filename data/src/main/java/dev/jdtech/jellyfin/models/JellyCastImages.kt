package dev.jdtech.jellyfin.models

import android.net.Uri
import dev.jdtech.jellyfin.repository.JellyfinRepository
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.ImageType

data class JellyCastImages(
    val primary: Uri? = null,
    val backdrop: Uri? = null,
    val logo: Uri? = null,
    val showPrimary: Uri? = null,
    val showBackdrop: Uri? = null,
    val showLogo: Uri? = null,
)

fun BaseItemDto.toJellyCastImages(
    jellyfinRepository: JellyfinRepository,
): JellyCastImages {
    val baseUrl = Uri.parse(jellyfinRepository.getBaseUrl())
    val primary = imageTags?.get(ImageType.PRIMARY)?.let { tag ->
        baseUrl.buildUpon()
            .appendEncodedPath("items/$id/Images/${ImageType.PRIMARY}")
            .appendQueryParameter("tag", tag)
            .build()
    }
    val backdrop = backdropImageTags?.firstOrNull()?.let { tag ->
        baseUrl.buildUpon()
            .appendEncodedPath("items/$id/Images/${ImageType.BACKDROP}/0")
            .appendQueryParameter("tag", tag)
            .build()
    }
    val logo = imageTags?.get(ImageType.LOGO)?.let { tag ->
        baseUrl.buildUpon()
            .appendEncodedPath("items/$id/Images/${ImageType.LOGO}")
            .appendQueryParameter("tag", tag)
            .build()
    }
    val showPrimary = seriesPrimaryImageTag?.let { tag ->
        baseUrl.buildUpon()
            .appendEncodedPath("items/$seriesId/Images/${ImageType.PRIMARY}")
            .appendQueryParameter("tag", tag)
            .build()
    }
    val showBackdrop = seriesPrimaryImageTag?.let { tag ->
        baseUrl.buildUpon()
            .appendEncodedPath("items/$seriesId/Images/${ImageType.BACKDROP}/0")
            .appendQueryParameter("tag", tag)
            .build()
    }
    val showLogo = seriesPrimaryImageTag?.let { tag ->
        baseUrl.buildUpon()
            .appendEncodedPath("items/$seriesId/Images/${ImageType.LOGO}")
            .appendQueryParameter("tag", tag)
            .build()
    }

    return JellyCastImages(
        primary = primary,
        backdrop = backdrop,
        logo = logo,
        showPrimary = showPrimary,
        showBackdrop = showBackdrop,
        showLogo = showLogo,
    )
}
