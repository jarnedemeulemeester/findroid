package dev.jdtech.jellyfin.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.jdtech.jellyfin.databinding.BaseItemBinding
import dev.jdtech.jellyfin.models.ViewItem
import org.jellyfin.sdk.model.api.BaseItemDto

class ViewItemListAdapter : ListAdapter<ViewItem, ViewItemListAdapter.ItemViewHolder>(DiffCallback) {
    class ItemViewHolder(private var binding: BaseItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(view: ViewItem) {
            binding.item = view
            binding.executePendingBindings()
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<ViewItem>() {
        override fun areItemsTheSame(oldItem: ViewItem, newItem: ViewItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ViewItem, newItem: ViewItem): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return ItemViewHolder(BaseItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }
}