package dev.jdtech.jellyfin.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.jdtech.jellyfin.databinding.CollectionItemBinding
import org.jellyfin.sdk.model.api.BaseItemDto

class CollectionListAdapter :
    ListAdapter<BaseItemDto, CollectionListAdapter.ViewViewHolder>(DiffCallback) {
    class ViewViewHolder(private var binding: CollectionItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(collection: BaseItemDto) {
            binding.collection = collection
            binding.executePendingBindings()
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<BaseItemDto>() {
        override fun areItemsTheSame(oldItem: BaseItemDto, newItem: BaseItemDto): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: BaseItemDto, newItem: BaseItemDto): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewViewHolder {
        return ViewViewHolder(
            CollectionItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewViewHolder, position: Int) {
        val collection = getItem(position)
        holder.bind(collection)
    }
}