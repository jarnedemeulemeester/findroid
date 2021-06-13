package dev.jdtech.jellyfin

import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.jdtech.jellyfin.database.Server
import dev.jdtech.jellyfin.adapters.ServerGridAdapter
import dev.jdtech.jellyfin.adapters.ViewListAdapter
import org.jellyfin.sdk.model.api.BaseItemDto

@BindingAdapter("servers")
fun bindServers(recyclerView: RecyclerView, data: List<Server>?) {
    val adapter = recyclerView.adapter as ServerGridAdapter
    adapter.submitList(data)
}

@BindingAdapter("views")
fun bindViews(recyclerView: RecyclerView, data: List<BaseItemDto>?) {
    val adapter = recyclerView.adapter as ViewListAdapter
    adapter.submitList(data)
}