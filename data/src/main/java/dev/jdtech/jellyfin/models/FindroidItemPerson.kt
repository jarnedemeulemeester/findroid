package dev.jdtech.jellyfin.models

import android.net.Uri
import dev.jdtech.jellyfin.repository.JellyfinRepository
import org.jellyfin.sdk.model.api.BaseItemPerson
import org.jellyfin.sdk.model.api.ImageType
import org.jellyfin.sdk.model.api.PersonKind
import java.util.UUID

data class FindroidItemPersonImage(
    val uri: Uri?,
    val blurHash: String?,
)

fun BaseItemPerson.toFindroidImage(
    repository: JellyfinRepository,
): FindroidItemPersonImage {
    val baseUrl = Uri.parse(repository.getBaseUrl())
    return FindroidItemPersonImage(
        uri = primaryImageTag?.let { tag ->
            baseUrl.buildUpon()
                .appendEncodedPath("items/$id/Images/${ImageType.PRIMARY}")
                .appendQueryParameter("tag", tag)
                .build()
        },
        blurHash = imageBlurHashes?.get(ImageType.PRIMARY)?.get(primaryImageTag),
    )
}

data class FindroidItemPerson(
    val id: UUID,
    val name: String,
    val type: PersonKind,
    val role: String,
    val image: FindroidItemPersonImage,
)

fun BaseItemPerson.toFindroidPerson(
    repository: JellyfinRepository,
): FindroidItemPerson {
    return FindroidItemPerson(
        id = id,
        name = name.orEmpty(),
        type = type,
        role = role.orEmpty(),
        image = toFindroidImage(repository),
    )
}
