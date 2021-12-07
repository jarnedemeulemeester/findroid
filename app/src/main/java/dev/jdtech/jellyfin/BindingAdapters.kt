package dev.jdtech.jellyfin

import android.view.View
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import dev.jdtech.jellyfin.adapters.DownloadsListAdapter
import dev.jdtech.jellyfin.adapters.HomeEpisodeListAdapter
import dev.jdtech.jellyfin.adapters.HomeItem
import dev.jdtech.jellyfin.adapters.PersonListAdapter
import dev.jdtech.jellyfin.adapters.ServerGridAdapter
import dev.jdtech.jellyfin.adapters.ViewItemListAdapter
import dev.jdtech.jellyfin.adapters.ViewListAdapter
import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.database.Server
import dev.jdtech.jellyfin.models.DownloadSection
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemPerson
import org.jellyfin.sdk.model.api.ImageType
import java.util.UUID

@BindingAdapter("servers")
fun bindServers(recyclerView: RecyclerView, data: List<Server>?) {
    val adapter = recyclerView.adapter as ServerGridAdapter
    adapter.submitList(data)
}

@BindingAdapter("views")
fun bindViews(recyclerView: RecyclerView, data: List<HomeItem>?) {
    val adapter = recyclerView.adapter as ViewListAdapter
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
        if (item.type == "Episode" || item.type == "Season" && item.imageTags.isNullOrEmpty()) item.seriesId else item.id

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

@BindingAdapter("people")
fun bindPeople(recyclerView: RecyclerView, data: List<BaseItemPerson>?) {
    val adapter = recyclerView.adapter as PersonListAdapter
    adapter.submitList(data)
}

@BindingAdapter("personImage")
fun bindPersonImage(imageView: ImageView, person: BaseItemPerson) {
    imageView
        .loadImage("/items/${person.id}/Images/${ImageType.PRIMARY}")
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

    if (!episode.imageTags.isNullOrEmpty()) { //TODO: Downloadmetadata currently does not store imagetags, so it always uses the backdrop
        when (episode.type) {
            "Movie" -> {
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
        if (episode.type == "Episode") {
            imageItemId = episode.seriesId!!
            imageType = ImageType.BACKDROP
        }
    }

    imageView
        .loadImage("/items/${imageItemId}/Images/$imageType")
        .posterDescription(episode.name)
}

@BindingAdapter("seasonPoster")
fun bindSeasonPoster(imageView: ImageView, seasonId: UUID) {
    imageView.loadImage("/items/${seasonId}/Images/${ImageType.PRIMARY}")
}

private fun ImageView.loadImage(url: String, errorPlaceHolderId: Int? = null): View {
    val api = JellyfinApi.getInstance(context.applicationContext)

    return Glide
        .with(context)
        .load("${api.api.baseUrl}$url")
        .transition(DrawableTransitionOptions.withCrossFade())
        .placeholder(R.color.neutral_800)
        .also { if (errorPlaceHolderId != null) error(errorPlaceHolderId) }
        .into(this)
        .view
}

private fun View.posterDescription(name: String?) {
    contentDescription = String.format(context.resources.getString(R.string.image_description_poster), name)
}

private fun View.backdropDescription(name: String?) {
    contentDescription = String.format(context.resources.getString(R.string.image_description_backdrop), name)
}