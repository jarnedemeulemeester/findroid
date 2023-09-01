package dev.jdtech.jellyfin

import android.view.View
import android.widget.ImageView
import androidx.annotation.DrawableRes
import coil.load
import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.FindroidMovie
import dev.jdtech.jellyfin.models.User
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.BaseItemPerson
import org.jellyfin.sdk.model.api.ImageType
import java.util.UUID
import dev.jdtech.jellyfin.core.R as CoreR

fun bindItemImage(imageView: ImageView, item: BaseItemDto) {
    val itemId =
        if (item.type == BaseItemKind.EPISODE || item.type == BaseItemKind.SEASON && item.imageTags.isNullOrEmpty()) item.seriesId else item.id

    imageView
        .loadImage("/items/$itemId/Images/${ImageType.PRIMARY}")
        .posterDescription(item.name)
}

fun bindItemImage(imageView: ImageView, item: FindroidItem) {
    val itemId = when (item) {
        is FindroidEpisode -> item.seriesId
        else -> item.id
    }

    imageView
        .loadImage("/items/$itemId/Images/${ImageType.PRIMARY}")
        .posterDescription(item.name)
}

fun bindItemBackdropImage(imageView: ImageView, item: FindroidItem?) {
    if (item == null) return

    imageView
        .loadImage("/items/${item.id}/Images/${ImageType.BACKDROP}")
        .backdropDescription(item.name)
}

fun bindItemBackdropById(imageView: ImageView, itemId: UUID) {
    imageView.loadImage("/items/$itemId/Images/${ImageType.BACKDROP}")
}

fun bindPersonImage(imageView: ImageView, person: BaseItemPerson) {
    imageView
        .loadImage("/items/${person.id}/Images/${ImageType.PRIMARY}", placeholderId = CoreR.drawable.person_placeholder)
        .posterDescription(person.name)
}

fun bindCardItemImage(imageView: ImageView, item: FindroidItem) {
    val imageType = when (item) {
        is FindroidMovie -> ImageType.BACKDROP
        else -> ImageType.PRIMARY
    }

    imageView
        .loadImage("/items/${item.id}/Images/$imageType")
        .posterDescription(item.name)
}

fun bindSeasonPoster(imageView: ImageView, seasonId: UUID) {
    imageView.loadImage("/items/$seasonId/Images/${ImageType.PRIMARY}")
}

fun bindUserImage(imageView: ImageView, user: User) {
    imageView
        .loadImage("/users/${user.id}/Images/${ImageType.PRIMARY}", placeholderId = CoreR.drawable.user_placeholder)
        .posterDescription(user.name)
}

private fun ImageView.loadImage(
    url: String,
    @DrawableRes placeholderId: Int = CoreR.color.neutral_800,
): View {
    val api = JellyfinApi.getInstance(context.applicationContext)

    this.load("${api.api.baseUrl}$url") {
        crossfade(true)
        placeholder(placeholderId)
        error(placeholderId)
    }

    return this
}

private fun View.posterDescription(name: String?) {
    contentDescription = context.resources.getString(CoreR.string.image_description_poster, name)
}

private fun View.backdropDescription(name: String?) {
    contentDescription = context.resources.getString(CoreR.string.image_description_backdrop, name)
}
