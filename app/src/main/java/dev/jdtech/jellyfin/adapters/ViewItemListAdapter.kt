package dev.jdtech.jellyfin.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.databinding.BaseItemBinding
import org.jellyfin.sdk.model.api.BaseItemDto

class ViewItemListAdapter(private val fixedWidth: Boolean = false) :
    ListAdapter<BaseItemDto, ViewItemListAdapter.ItemViewHolder>(DiffCallback) {

    class ItemViewHolder(private var binding: BaseItemBinding, private val parent: ViewGroup) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: BaseItemDto, fixedWidth: Boolean) {
            binding.item = item
            binding.itemName.text = if (item.type == "Episode") item.seriesName else item.name
            binding.itemCount.visibility =
                if (item.userData?.unplayedItemCount != null && item.userData?.unplayedItemCount!! > 0) View.VISIBLE else View.GONE
            if (fixedWidth) {
                binding.itemLayout.layoutParams.width =
                    parent.resources.getDimension(R.dimen.overview_media_width).toInt()
                (binding.itemLayout.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin = 0
            }
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return ItemViewHolder(
            BaseItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ), parent
        )
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, fixedWidth)
    }
}