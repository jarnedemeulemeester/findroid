package dev.jdtech.jellyfin.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.jdtech.jellyfin.databinding.CollectionItemBinding
import org.jellyfin.sdk.model.api.BaseItemDto

class CollectionListAdapter(
    private val onClickListener: OnClickListener
) : ListAdapter<BaseItemDto, CollectionListAdapter.CollectionViewHolder>(DiffCallback) {
    class CollectionViewHolder(private var binding: CollectionItemBinding) :
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CollectionViewHolder {
        return CollectionViewHolder(
            CollectionItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: CollectionViewHolder, position: Int) {
        val collection = getItem(position)
        holder.itemView.setOnClickListener {
            onClickListener.onClick(collection)
        }
        holder.bind(collection)
    }

    class OnClickListener(val clickListener: (collection: BaseItemDto) -> Unit) {
        fun onClick(collection: BaseItemDto) = clickListener(collection)
    }
}