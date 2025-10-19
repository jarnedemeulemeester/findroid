package dev.jdtech.jellyfin.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.jdtech.jellyfin.bindItemImage
import dev.jdtech.jellyfin.databinding.BaseItemBinding
import dev.jdtech.jellyfin.models.JellyCastItem
import dev.jdtech.jellyfin.models.JellyCastMovie
import dev.jdtech.jellyfin.models.JellyCastShow
import dev.jdtech.jellyfin.models.JellyCastEpisode
import dev.jdtech.jellyfin.models.isDownloaded
import dev.jdtech.jellyfin.models.isDownloading

class DownloadsAdapter(
    private val onItemClickListener: (item: JellyCastItem) -> Unit,
    private val onItemLongClickListener: (item: JellyCastItem) -> Unit,
) : ListAdapter<JellyCastItem, DownloadsAdapter.ItemViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = BaseItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        android.util.Log.d("DownloadsAdapter", "onCreateViewHolder called")
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = getItem(position)
        android.util.Log.d("DownloadsAdapter", "onBindViewHolder position=$position item=${item.name}")
        holder.bind(item)
    }

    inner class ItemViewHolder(private val binding: BaseItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: JellyCastItem) {
            val itemName = when (item) {
                is JellyCastMovie -> item.name
                is JellyCastShow -> item.name
                is JellyCastEpisode -> item.name
                else -> ""
            }
            
            // DEBUG: Show sources count in the name
            val debugInfo = "(sources=${item.sources.size})"
            binding.itemName.text = "$itemName $debugInfo"
            android.util.Log.d("DownloadsAdapter", "bind: $itemName, sources=${item.sources.size}")

            // Load image using bindItemImage helper function
            try {
                bindItemImage(binding.itemImage, item)
                android.util.Log.d("DownloadsAdapter", "Image loaded for: $itemName")
            } catch (e: Exception) {
                android.util.Log.e("DownloadsAdapter", "Failed to load image for $itemName", e)
                // Set a solid background color if image loading fails
                binding.itemImage.setBackgroundColor(android.graphics.Color.parseColor("#505050"))
            }

            // Show download status
            val isDownloaded = item.isDownloaded()
            val isDownloading = item.isDownloading()
            
            binding.downloadedIcon.isVisible = isDownloaded
            binding.downloadProgress.isVisible = isDownloading
            
            if (isDownloading) {
                // TODO: Show actual download progress if available
                binding.downloadProgress.text = "..."
            }

            // Item count (for shows)
            if (item is JellyCastShow) {
                val downloadedCount = item.sources.size
                binding.itemCount.isVisible = downloadedCount > 0
                binding.itemCount.text = downloadedCount.toString()
            } else {
                binding.itemCount.isVisible = false
            }

            // Normal styling restored
            
            // Click listeners
            binding.root.setOnClickListener { onItemClickListener(item) }
            binding.root.setOnLongClickListener {
                onItemLongClickListener(item)
                true
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<JellyCastItem>() {
            override fun areItemsTheSame(oldItem: JellyCastItem, newItem: JellyCastItem): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: JellyCastItem, newItem: JellyCastItem): Boolean {
                return oldItem == newItem
            }
        }
    }
}
