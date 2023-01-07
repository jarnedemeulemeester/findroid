package dev.jdtech.jellyfin

import android.view.View
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import dev.jdtech.jellyfin.adapters.HomeEpisodeListAdapter
import dev.jdtech.jellyfin.adapters.ServerGridAdapter
import dev.jdtech.jellyfin.adapters.ViewItemListAdapter
import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.models.Server
import dev.jdtech.jellyfin.models.User
import java.util.UUID
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.BaseItemPerson
import org.jellyfin.sdk.model.api.ImageType

@BindingAdapter("servers")
fun bindServers(recyclerView: RecyclerView, data: List<Server>?) {
    val adapter = recyclerView.adapter as ServerGridAdapter
    adapter.submitList(data)
}

@BindingAdapter("items")
fun bindItems(recyclerView: RecyclerView, data: List<BaseItemDto>?) {
    val adapter = recyclerView.adapter as ViewItemListAdapter
    adapter.submitList(data)
}

@BindingAdapter("itemImage")
fun bindItemImage(imageView: ImageView, item: BaseItemDto) {
    val itemId =
        if (item.type == BaseItemKind.EPISODE || item.type == BaseItemKind.SEASON && item.imageTags.isNullOrEmpty()) item.seriesId else item.id

    imageView
        .loadImage("/items/$itemId/Images/${ImageType.PRIMARY}")
        .posterDescription(item.name)
}

@BindingAdapter("itemBackdropImage")
fun bindItemBackdropImage(imageView: ImageView, item: BaseItemDto?) {
    if (item == null) return

    imageView
        .loadImage("/items/${item.id}/Images/${ImageType.BACKDROP}")
        .backdropDescription(item.name)
}

@BindingAdapter("itemBackdropById")
fun bindItemBackdropById(imageView: ImageView, itemId: UUID) {
    imageView.loadImage("/items/$itemId/Images/${ImageType.BACKDROP}")
}

@BindingAdapter("personImage")
fun bindPersonImage(imageView: ImageView, person: BaseItemPerson) {
    imageView
        .loadImage("/items/${person.id}/Images/${ImageType.PRIMARY}", placeholderId = R.drawable.person_placeholder)
        .posterDescription(person.name)
}

@BindingAdapter("homeEpisodes")
fun bindHomeEpisodes(recyclerView: RecyclerView, data: List<BaseItemDto>?) {
    val adapter = recyclerView.adapter as HomeEpisodeListAdapter
    adapter.submitList(data)
}

@BindingAdapter("baseItemImage")
fun bindBaseItemImage(imageView: ImageView, episode: BaseItemDto?) {
    if (episode == null) return

    var imageItemId = episode.id
    var imageType = ImageType.PRIMARY

    if (!episode.imageTags.isNullOrEmpty()) { // TODO: Downloadmetadata currently does not store imagetags, so it always uses the backdrop
        when (episode.type) {
            BaseItemKind.MOVIE -> {
                if (!episode.backdropImageTags.isNullOrEmpty()) {
                    imageType = ImageType.BACKDROP
                }
            }
            else -> {
                if (!episode.imageTags!!.keys.contains(ImageType.PRIMARY)) {
                    imageType = ImageType.BACKDROP
                }
            }
        }
    } else {
        if (episode.type == BaseItemKind.EPISODE) {
            imageItemId = episode.seriesId!!
            imageType = ImageType.BACKDROP
        }
    }

    imageView
        .loadImage("/items/$imageItemId/Images/$imageType")
        .posterDescription(episode.name)
}

@BindingAdapter("seasonPoster")
fun bindSeasonPoster(imageView: ImageView, seasonId: UUID) {
    imageView.loadImage("/items/$seasonId/Images/${ImageType.PRIMARY}")
}

@BindingAdapter("userImage")
fun bindUserImage(imageView: ImageView, user: User) {
    imageView
        .loadImage("/users/${user.id}/Images/${ImageType.PRIMARY}", placeholderId = R.drawable.user_placeholder)
        .posterDescription(user.name)
}

private fun ImageView.loadImage(
    url: String,
    @DrawableRes placeholderId: Int = R.color.neutral_800,
    @DrawableRes errorPlaceHolderId: Int? = null
): View {
    val api = JellyfinApi.getInstance(context.applicationContext)

    Glide
        .with(context)
        .load("${api.api.baseUrl}$url")
        .transition(DrawableTransitionOptions.withCrossFade())
        .placeholder(placeholderId)
        .error(errorPlaceHolderId)
        .into(this)

    return this
}

private fun View.posterDescription(name: String?) {
    contentDescription = context.resources.getString(R.string.image_description_poster, name)
}

private fun View.backdropDescription(name: String?) {
    contentDescription = context.resources.getString(R.string.image_description_backdrop, name)
}
