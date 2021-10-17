package dev.jdtech.jellyfin.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.jdtech.jellyfin.databinding.StarredInSectionBinding
import dev.jdtech.jellyfin.models.StarredIn
import org.jellyfin.sdk.model.api.BaseItemDto

internal class StarredInAdapter(private val onClick: (BaseItemDto) -> Unit) :
    ListAdapter<StarredIn, RecyclerView.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return StarredInViewHolder(
            StarredInSectionBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is StarredInViewHolder) {
            getItem(position).also { starredIn ->
                holder.bind(starredIn)
                holder.itemView.setOnClickListener { onClick(starredIn.dto) }
            }
        }
    }

    class StarredInViewHolder(private val binding: StarredInSectionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(starredIn: StarredIn) {
            binding.starredIn = starredIn
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<StarredIn>() {
        override fun areItemsTheSame(oldItem: StarredIn, newItem: StarredIn): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: StarredIn, newItem: StarredIn): Boolean {
            return oldItem == newItem
        }
    }
}
