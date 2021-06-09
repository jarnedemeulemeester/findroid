package dev.jdtech.jellyfin

import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.jdtech.jellyfin.database.Server
import dev.jdtech.jellyfin.adapters.ServerGridAdapter

@BindingAdapter("listData")
fun bindRecyclerView(recyclerView: RecyclerView, data: List<Server>?) {
    val adapter = recyclerView.adapter as ServerGridAdapter
    adapter.submitList(data)
}