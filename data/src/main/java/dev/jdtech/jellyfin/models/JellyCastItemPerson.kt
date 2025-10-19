package dev.jdtech.jellyfin.models

import android.net.Uri
import dev.jdtech.jellyfin.repository.JellyfinRepository
import org.jellyfin.sdk.model.api.BaseItemPerson
import org.jellyfin.sdk.model.api.ImageType
import org.jellyfin.sdk.model.api.PersonKind
import java.util.UUID

data class JellyCastItemPersonImage(
    val uri: Uri?,
    val blurHash: String?,
)

fun BaseItemPerson.toJellyCastImage(
    repository: JellyfinRepository,
): JellyCastItemPersonImage {
    val baseUrl = Uri.parse(repository.getBaseUrl())
    return JellyCastItemPersonImage(
        uri = primaryImageTag?.let { tag ->
            baseUrl.buildUpon()
                .appendEncodedPath("items/$id/Images/${ImageType.PRIMARY}")
                .appendQueryParameter("tag", tag)
                .build()
        },
        blurHash = imageBlurHashes?.get(ImageType.PRIMARY)?.get(primaryImageTag),
    )
}

data class JellyCastItemPerson(
    val id: UUID,
    val name: String,
    val type: PersonKind,
    val role: String,
    val image: JellyCastItemPersonImage,
)

fun BaseItemPerson.toJellyCastPerson(
    repository: JellyfinRepository,
): JellyCastItemPerson {
    return JellyCastItemPerson(
        id = id,
        name = name.orEmpty(),
        type = type,
        role = role.orEmpty(),
        image = toJellyCastImage(repository),
    )
}
