package dev.jdtech.jellyfin.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.databinding.BaseItemBinding
import dev.jdtech.jellyfin.models.PlayerItem
import dev.jdtech.jellyfin.utils.downloadMetadataToBaseItemDto

class DownloadViewItemListAdapter(
    private val onClickListener: OnClickListener,
    private val fixedWidth: Boolean = false,
) : ListAdapter<PlayerItem, DownloadViewItemListAdapter.ItemViewHolder>(DiffCallback) {

    class ItemViewHolder(private var binding: BaseItemBinding, private val parent: ViewGroup) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: PlayerItem, fixedWidth: Boolean) {
            val metadata = item.item!!
            binding.item = downloadMetadataToBaseItemDto(metadata)
            binding.itemName.text = item.name
            binding.itemCount.visibility = View.GONE
            if (fixedWidth) {
                binding.itemLayout.layoutParams.width = parent.resources.getDimension(CoreR.dimen.overview_media_width).toInt()
                (binding.itemLayout.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin = 0
            }
            binding.executePendingBindings()
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<PlayerItem>() {
        override fun areItemsTheSame(oldItem: PlayerItem, newItem: PlayerItem): Boolean {
            return oldItem.itemId == newItem.itemId
        }

        override fun areContentsTheSame(oldItem: PlayerItem, newItem: PlayerItem): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return ItemViewHolder(
            BaseItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ),
            parent
        )
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = getItem(position)
        holder.itemView.setOnClickListener {
            onClickListener.onClick(item)
        }
        holder.bind(item, fixedWidth)
    }

    class OnClickListener(val clickListener: (item: PlayerItem) -> Unit) {
        fun onClick(item: PlayerItem) = clickListener(item)
    }
}
