package dev.jdtech.jellyfin.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.jdtech.jellyfin.databinding.CollectionItemBinding
import dev.jdtech.jellyfin.models.FindroidCollection

class CollectionListAdapter(
    private val onClickListener: OnClickListener,
) : ListAdapter<FindroidCollection, CollectionListAdapter.CollectionViewHolder>(DiffCallback) {
    class CollectionViewHolder(private var binding: CollectionItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(collection: FindroidCollection) {
            binding.collection = collection
            binding.executePendingBindings()
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<FindroidCollection>() {
        override fun areItemsTheSame(oldItem: FindroidCollection, newItem: FindroidCollection): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: FindroidCollection, newItem: FindroidCollection): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CollectionViewHolder {
        return CollectionViewHolder(
            CollectionItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            ),
        )
    }

    override fun onBindViewHolder(holder: CollectionViewHolder, position: Int) {
        val collection = getItem(position)
        holder.itemView.setOnClickListener {
            onClickListener.onClick(collection)
        }
        holder.bind(collection)
    }

    class OnClickListener(val clickListener: (collection: FindroidCollection) -> Unit) {
        fun onClick(collection: FindroidCollection) = clickListener(collection)
    }
}
