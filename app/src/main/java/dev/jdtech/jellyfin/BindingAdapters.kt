package dev.jdtech.jellyfin

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import dev.jdtech.jellyfin.adapters.*
import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.database.Server
import dev.jdtech.jellyfin.models.FavoriteSection
import dev.jdtech.jellyfin.models.StarredIn
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemPerson
import org.jellyfin.sdk.model.api.ImageType
import java.util.*

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

@BindingAdapter("starredIn")
fun bindStarredIn(recyclerView: RecyclerView, data: List<StarredIn>?) {
    val adapter = recyclerView.adapter as StarredInAdapter
    adapter.submitList(data)
}

@BindingAdapter("items")
fun bindItems(recyclerView: RecyclerView, data: List<BaseItemDto>?) {
    val adapter = recyclerView.adapter as ViewItemListAdapter
    adapter.submitList(data)
}

@BindingAdapter("itemImage")
fun bindItemImage(imageView: ImageView, item: BaseItemDto) {
    val jellyfinApi = JellyfinApi.getInstance(imageView.context.applicationContext, "")

    val itemId =
        if (item.type == "Episode" || item.type == "Season" && item.imageTags.isNullOrEmpty()) item.seriesId else item.id

    Glide
        .with(imageView.context)
        .load(jellyfinApi.api.baseUrl.plus("/items/${itemId}/Images/${ImageType.PRIMARY}"))
        .transition(DrawableTransitionOptions.withCrossFade())
        .placeholder(R.color.neutral_800)
        .into(imageView)

    imageView.contentDescription = "${item.name} poster"
}

@BindingAdapter("itemBackdropImage")
fun bindItemBackdropImage(imageView: ImageView, item: BaseItemDto?) {
    if (item == null) return
    val jellyfinApi = JellyfinApi.getInstance(imageView.context.applicationContext, "")

    Glide
        .with(imageView.context)
        .load(jellyfinApi.api.baseUrl.plus("/items/${item.id}/Images/${ImageType.BACKDROP}"))
        .transition(DrawableTransitionOptions.withCrossFade())
        .into(imageView)

    imageView.contentDescription = "${item.name} backdrop"
}

@BindingAdapter("itemBackdropById")
fun bindItemBackdropById(imageView: ImageView, itemId: UUID) {
    val jellyfinApi = JellyfinApi.getInstance(imageView.context.applicationContext, "")

    Glide
        .with(imageView.context)
        .load(jellyfinApi.api.baseUrl.plus("/items/${itemId}/Images/${ImageType.BACKDROP}"))
        .transition(DrawableTransitionOptions.withCrossFade())
        .into(imageView)
}

@BindingAdapter("collections")
fun bindCollections(recyclerView: RecyclerView, data: List<BaseItemDto>?) {
    val adapter = recyclerView.adapter as CollectionListAdapter
    adapter.submitList(data)
}

@BindingAdapter("people")
fun bindPeople(recyclerView: RecyclerView, data: List<BaseItemPerson>?) {
    val adapter = recyclerView.adapter as PersonListAdapter
    adapter.submitList(data)
}

@BindingAdapter("personImage")
fun bindPersonImage(imageView: ImageView, person: BaseItemPerson) {
    val jellyfinApi = JellyfinApi.getInstance(imageView.context.applicationContext, "")

    Glide
        .with(imageView.context)
        .load(jellyfinApi.api.baseUrl.plus("/items/${person.id}/Images/${ImageType.PRIMARY}"))
        .transition(DrawableTransitionOptions.withCrossFade())
        .placeholder(R.color.neutral_800)
        .into(imageView)

    imageView.contentDescription = "${person.name} poster"
}

@BindingAdapter("episodes")
fun bindEpisodes(recyclerView: RecyclerView, data: List<EpisodeItem>?) {
    val adapter = recyclerView.adapter as EpisodeListAdapter
    adapter.submitList(data)
}

@BindingAdapter("homeEpisodes")
fun bindHomeEpisodes(recyclerView: RecyclerView, data: List<BaseItemDto>?) {
    val adapter = recyclerView.adapter as HomeEpisodeListAdapter
    adapter.submitList(data)
}

@BindingAdapter("baseItemImage")
fun bindBaseItemImage(imageView: ImageView, episode: BaseItemDto?) {
    if (episode == null) return

    val jellyfinApi = JellyfinApi.getInstance(imageView.context.applicationContext, "")

    var imageItemId = episode.id
    var imageType = ImageType.PRIMARY

    if (!episode.imageTags.isNullOrEmpty()) {
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

    Glide
        .with(imageView.context)
        .load(jellyfinApi.api.baseUrl.plus("/items/${imageItemId}/Images/$imageType"))
        .transition(DrawableTransitionOptions.withCrossFade())
        .placeholder(R.color.neutral_800)
        .into(imageView)

    imageView.contentDescription = "${episode.name} poster"
}

@BindingAdapter("seasonPoster")
fun bindSeasonPoster(imageView: ImageView, seasonId: UUID) {
    val jellyfinApi = JellyfinApi.getInstance(imageView.context.applicationContext, "")

    Glide
        .with(imageView.context)
        .load(jellyfinApi.api.baseUrl.plus("/items/${seasonId}/Images/${ImageType.PRIMARY}"))
        .transition(DrawableTransitionOptions.withCrossFade())
        .placeholder(R.color.neutral_800)
        .into(imageView)
}

@BindingAdapter("favoriteSections")
fun bindFavoriteSections(recyclerView: RecyclerView, data: List<FavoriteSection>?) {
    val adapter = recyclerView.adapter as FavoritesListAdapter
    adapter.submitList(data)
}