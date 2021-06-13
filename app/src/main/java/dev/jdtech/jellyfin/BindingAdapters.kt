package dev.jdtech.jellyfin

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import dev.jdtech.jellyfin.database.Server
import dev.jdtech.jellyfin.adapters.ServerGridAdapter
import dev.jdtech.jellyfin.adapters.ViewItemListAdapter
import dev.jdtech.jellyfin.adapters.ViewListAdapter
import dev.jdtech.jellyfin.models.View
import dev.jdtech.jellyfin.models.ViewItem
import org.jellyfin.sdk.model.api.BaseItemDto

@BindingAdapter("servers")
fun bindServers(recyclerView: RecyclerView, data: List<Server>?) {
    val adapter = recyclerView.adapter as ServerGridAdapter
    adapter.submitList(data)
}

@BindingAdapter("views")
fun bindViews(recyclerView: RecyclerView, data: List<View>?) {
    val adapter = recyclerView.adapter as ViewListAdapter
    adapter.submitList(data)
}

@BindingAdapter("items")
fun bindItems(recyclerView: RecyclerView, data: List<ViewItem>?) {
    val adapter = recyclerView.adapter as ViewItemListAdapter
    adapter.submitList(data)
}

@BindingAdapter("itemImage")
fun bindItemImage(imageView: ImageView, item: ViewItem) {
    Glide
        .with(imageView.context)
        .load(item.primaryImageUrl)
        .placeholder(ColorDrawable(Color.GRAY))
        .into(imageView)
}