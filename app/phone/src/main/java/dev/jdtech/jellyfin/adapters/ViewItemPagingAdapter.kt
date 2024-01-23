package dev.jdtech.jellyfin.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import dev.jdtech.jellyfin.bindItemImage
import dev.jdtech.jellyfin.databinding.BaseItemBinding
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.isDownloaded
import dev.jdtech.jellyfin.core.R as CoreR

class ViewItemPagingAdapter(
    private val onClickListener: (item: FindroidItem) -> Unit,
    private val fixedWidth: Boolean = false,
) : PagingDataAdapter<FindroidItem, ViewItemPagingAdapter.ItemViewHolder>(DiffCallback) {

    class ItemViewHolder(private var binding: BaseItemBinding, private val parent: ViewGroup) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: FindroidItem, fixedWidth: Boolean) {
            binding.itemName.text =
                if (item is FindroidEpisode) item.seriesName else item.name
            binding.itemCount.visibility =
                if (item.unplayedItemCount != null && item.unplayedItemCount!! > 0) View.VISIBLE else View.GONE
            if (fixedWidth) {
                binding.itemLayout.layoutParams.width =
                    parent.resources.getDimension(CoreR.dimen.overview_media_width).toInt()
                (binding.itemLayout.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin = 0
            }

            binding.itemCount.text = item.unplayedItemCount.toString()
            binding.playedIcon.isVisible = item.played
            binding.downloadedIcon.isVisible = item.isDownloaded()

            bindItemImage(binding.itemImage, item)
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<FindroidItem>() {
        override fun areItemsTheSame(oldItem: FindroidItem, newItem: FindroidItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: FindroidItem, newItem: FindroidItem): Boolean {
            return oldItem.name == newItem.name
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return ItemViewHolder(
            BaseItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            ),
            parent,
        )
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = getItem(position)
        if (item != null) {
            holder.itemView.setOnClickListener {
                onClickListener(item)
            }
            holder.bind(item, fixedWidth)
        }
    }
}
