package dev.jdtech.jellyfin.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.jdtech.jellyfin.databinding.ViewItemBinding
import org.jellyfin.sdk.model.api.BaseItemDto

class ViewListAdapter : ListAdapter<BaseItemDto, ViewListAdapter.ViewHolder>(DiffCallback) {
    class ViewHolder(private var binding: ViewItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(view: BaseItemDto) {
            binding.view = view
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ViewItemBinding.inflate(LayoutInflater.from(parent.context)))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val view = getItem(position)
        holder.bind(view)
    }
}