package dev.jdtech.jellyfin.models

import android.net.Uri
import dev.jdtech.jellyfin.repository.JellyfinRepository
import java.util.UUID
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.ImageType

data class FindroidImage(val uri: Uri?, val blurHash: String?)

data class FindroidImages(
    val primary: FindroidImage? = null,
    val backdrop: FindroidImage? = null,
    val logo: FindroidImage? = null,
    val showPrimary: FindroidImage? = null,
    val showBackdrop: FindroidImage? = null,
    val showLogo: FindroidImage? = null,
)

fun BaseItemDto.toFindroidImages(jellyfinRepository: JellyfinRepository): FindroidImages {
    val baseUrl = Uri.parse(jellyfinRepository.getBaseUrl())
    val primary = imageTags?.get(ImageType.PRIMARY)?.let { tag ->
        FindroidImage(
            uri = baseUrl
                .buildUpon()
                .appendEncodedPath("items/$id/Images/${ImageType.PRIMARY}")
                .appendQueryParameter("tag", tag)
                .build(),
            blurHash = imageBlurHashes?.get(ImageType.PRIMARY)?.let {
                it[tag] ?: it.values.firstOrNull()
            },
        )
    }
    val backdrop = (backdropImageTags?.firstOrNull() ?: imageTags?.get(ImageType.BACKDROP))?.let { tag ->
        FindroidImage(
            uri = baseUrl
                .buildUpon()
                .appendEncodedPath("items/$id/Images/${ImageType.BACKDROP}/0")
                .appendQueryParameter("tag", tag)
                .build(),
            blurHash = imageBlurHashes?.get(ImageType.BACKDROP)?.let {
                it[tag] ?: it.values.firstOrNull()
            },
        )
    }
    val logo = imageTags?.get(ImageType.LOGO)?.let { tag ->
        FindroidImage(
            uri = baseUrl
                .buildUpon()
                .appendEncodedPath("items/$id/Images/${ImageType.LOGO}")
                .appendQueryParameter("tag", tag)
                .build(),
            blurHash = imageBlurHashes?.get(ImageType.LOGO)?.let {
                it[tag] ?: it.values.firstOrNull()
            },
        )
    }
    val showPrimary = seriesPrimaryImageTag?.let { tag ->
        FindroidImage(
            uri = baseUrl
                .buildUpon()
                .appendEncodedPath("items/$seriesId/Images/${ImageType.PRIMARY}")
                .appendQueryParameter("tag", tag)
                .build(),
            blurHash = imageBlurHashes?.get(ImageType.PRIMARY)?.let {
                it[tag] ?: it.values.firstOrNull()
            },
        )
    }
    val showBackdrop = seriesPrimaryImageTag?.let { tag ->
        FindroidImage(
            uri = baseUrl
                .buildUpon()
                .appendEncodedPath("items/$seriesId/Images/${ImageType.BACKDROP}/0")
                .appendQueryParameter("tag", tag)
                .build(),
            blurHash = imageBlurHashes?.get(ImageType.BACKDROP)?.let {
                it[tag] ?: it.values.firstOrNull()
            },
        )
    }
    val showLogo = seriesPrimaryImageTag?.let { tag ->
        FindroidImage(
            uri = baseUrl
                .buildUpon()
                .appendEncodedPath("items/$seriesId/Images/${ImageType.LOGO}")
                .appendQueryParameter("tag", tag)
                .build(),
            blurHash = imageBlurHashes?.get(ImageType.LOGO)?.let {
                it[tag] ?: it.values.firstOrNull()
            },
        )
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
        primary = primaryBlurHash?.let {
            FindroidImage(
                uri = Uri.Builder().appendEncodedPath("images/$itemId/primary").build(),
                blurHash = it,
            )
        },
        backdrop = backdropBlurHash?.let {
            FindroidImage(
                uri = Uri.Builder().appendEncodedPath("images/$itemId/backdrop").build(),
                blurHash = it,
            )
        },
        logo = logoBlurHash?.let {
            FindroidImage(
                uri = Uri.Builder().appendEncodedPath("images/$itemId/logo").build(),
                blurHash = it,
            )
        },
    )
}

fun FindroidShowDto.toLocalFindroidImages(itemId: UUID): FindroidImages {
    return FindroidImages(
        primary = primaryBlurHash?.let {
            FindroidImage(
                uri = Uri.Builder().appendEncodedPath("images/$itemId/primary").build(),
                blurHash = it,
            )
        },
        backdrop = backdropBlurHash?.let {
            FindroidImage(
                uri = Uri.Builder().appendEncodedPath("images/$itemId/backdrop").build(),
                blurHash = it,
            )
        },
        logo = logoBlurHash?.let {
            FindroidImage(
                uri = Uri.Builder().appendEncodedPath("images/$itemId/logo").build(),
                blurHash = it,
            )
        },
    )
}

fun FindroidSeasonDto.toLocalFindroidImages(itemId: UUID): FindroidImages {
    return FindroidImages(
        primary = primaryBlurHash?.let {
            FindroidImage(
                uri = Uri.Builder().appendEncodedPath("images/$itemId/primary").build(),
                blurHash = it,
            )
        },
        backdrop = backdropBlurHash?.let {
            FindroidImage(
                uri = Uri.Builder().appendEncodedPath("images/$itemId/backdrop").build(),
                blurHash = it,
            )
        },
        logo = logoBlurHash?.let {
            FindroidImage(
                uri = Uri.Builder().appendEncodedPath("images/$itemId/logo").build(),
                blurHash = it,
            )
        },
        showPrimary = showPrimaryBlurHash?.let {
            FindroidImage(
                uri = Uri.Builder().appendEncodedPath("images/$seriesId/primary").build(),
                blurHash = it,
            )
        },
        showBackdrop = showBackdropBlurHash?.let {
            FindroidImage(
                uri = Uri.Builder().appendEncodedPath("images/$seriesId/backdrop").build(),
                blurHash = it,
            )
        },
        showLogo = showLogoBlurHash?.let {
            FindroidImage(
                uri = Uri.Builder().appendEncodedPath("images/$seriesId/logo").build(),
                blurHash = it,
            )
        },
    )
}

fun FindroidEpisodeDto.toLocalFindroidImages(itemId: UUID): FindroidImages {
    return FindroidImages(
        primary = primaryBlurHash?.let {
            FindroidImage(
                uri = Uri.Builder().appendEncodedPath("images/$itemId/primary").build(),
                blurHash = it,
            )
        },
        backdrop = backdropBlurHash?.let {
            FindroidImage(
                uri = Uri.Builder().appendEncodedPath("images/$itemId/backdrop").build(),
                blurHash = it,
            )
        },
        logo = logoBlurHash?.let {
            FindroidImage(
                uri = Uri.Builder().appendEncodedPath("images/$itemId/logo").build(),
                blurHash = it,
            )
        },
        showPrimary = showPrimaryBlurHash?.let {
            FindroidImage(
                uri = Uri.Builder().appendEncodedPath("images/$seriesId/primary").build(),
                blurHash = it,
            )
        },
        showBackdrop = showBackdropBlurHash?.let {
            FindroidImage(
                uri = Uri.Builder().appendEncodedPath("images/$seriesId/backdrop").build(),
                blurHash = it,
            )
        },
        showLogo = showLogoBlurHash?.let {
            FindroidImage(
                uri = Uri.Builder().appendEncodedPath("images/$seriesId/logo").build(),
                blurHash = it,
            )
        },
    )
}
