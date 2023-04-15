package dev.jdtech.jellyfin.adapters

import android.content.res.Resources
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.databinding.StorageItemBinding
import dev.jdtech.jellyfin.models.StorageItem
import kotlin.math.roundToInt

class StorageListAdapter :
    ListAdapter<StorageItem, StorageListAdapter.ItemViewHolder>(DiffCallback) {

    class ItemViewHolder(private var binding: StorageItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(storageItem: StorageItem) {
            binding.name.text = storageItem.item.name
            (binding.mainItemLayout.layoutParams as MarginLayoutParams).marginStart = (storageItem.indent + 1) * 12.dp
            (binding.introTimestampsLayout.layoutParams as MarginLayoutParams).marginStart = (storageItem.indent + 1) * 12.dp
            (binding.trickPlayLayout.layoutParams as MarginLayoutParams).marginStart = (storageItem.indent + 1) * 12.dp
            if (storageItem.size != null) {
                binding.size.text = binding.root.context.getString(CoreR.string.mega_byte_suffix, storageItem.size)
            }
            binding.introTimestampsLayout.isVisible = storageItem.introTimestamps
            binding.trickPlayLayout.isVisible = storageItem.trickPlayData
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<StorageItem>() {
        override fun areItemsTheSame(oldItem: StorageItem, newItem: StorageItem): Boolean {
            return oldItem.item.id == newItem.item.id
        }

        override fun areContentsTheSame(oldItem: StorageItem, newItem: StorageItem): Boolean {
            return false
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return ItemViewHolder(
            StorageItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }
}

val Int.dp: Int
    get() = (this * Resources.getSystem().displayMetrics.density).roundToInt()
