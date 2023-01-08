package dev.jdtech.jellyfin.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.databinding.BaseItemBinding
import dev.jdtech.jellyfin.models.DownloadSeriesMetadata
import dev.jdtech.jellyfin.utils.downloadSeriesMetadataToBaseItemDto

class DownloadSeriesListAdapter(
    private val onClickListener: OnClickListener,
    private val fixedWidth: Boolean = false,
) : ListAdapter<dev.jdtech.jellyfin.models.DownloadSeriesMetadata, DownloadSeriesListAdapter.ItemViewHolder>(DiffCallback) {

    class ItemViewHolder(private var binding: BaseItemBinding, private val parent: ViewGroup) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: dev.jdtech.jellyfin.models.DownloadSeriesMetadata, fixedWidth: Boolean) {
            binding.item = downloadSeriesMetadataToBaseItemDto(item)
            binding.itemName.text = item.name
            binding.itemCount.text = item.episodes.size.toString()
            if (fixedWidth) {
                binding.itemLayout.layoutParams.width =
                    parent.resources.getDimension(R.dimen.overview_media_width).toInt()
                (binding.itemLayout.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin = 0
            }
            binding.executePendingBindings()
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<dev.jdtech.jellyfin.models.DownloadSeriesMetadata>() {
        override fun areItemsTheSame(
            oldItem: dev.jdtech.jellyfin.models.DownloadSeriesMetadata,
            newItem: dev.jdtech.jellyfin.models.DownloadSeriesMetadata
        ): Boolean {
            return oldItem.itemId == newItem.itemId
        }

        override fun areContentsTheSame(
            oldItem: dev.jdtech.jellyfin.models.DownloadSeriesMetadata,
            newItem: dev.jdtech.jellyfin.models.DownloadSeriesMetadata
        ): Boolean {
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

    class OnClickListener(val clickListener: (item: dev.jdtech.jellyfin.models.DownloadSeriesMetadata) -> Unit) {
        fun onClick(item: dev.jdtech.jellyfin.models.DownloadSeriesMetadata) = clickListener(item)
    }
}
