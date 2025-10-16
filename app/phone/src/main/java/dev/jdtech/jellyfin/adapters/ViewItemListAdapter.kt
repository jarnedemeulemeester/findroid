package dev.jdtech.jellyfin.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.internal.managers.ViewComponentManager
import dev.jdtech.jellyfin.bindItemImage
import dev.jdtech.jellyfin.databinding.BaseItemBinding
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.isDownloaded
import dev.jdtech.jellyfin.models.isDownloading
import dev.jdtech.jellyfin.presentation.downloads.DownloaderEntryPoint
import dev.jdtech.jellyfin.utils.Downloader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import dev.jdtech.jellyfin.core.R as CoreR
import timber.log.Timber

class ViewItemListAdapter(
    private val onClickListener: (item: FindroidItem) -> Unit,
    private val fixedWidth: Boolean = false,
    private val onLongClickListener: ((item: FindroidItem) -> Unit)? = null,
) : ListAdapter<FindroidItem, ViewItemListAdapter.ItemViewHolder>(DiffCallback) {

    class ItemViewHolder(private var binding: BaseItemBinding, private val parent: ViewGroup) :
        RecyclerView.ViewHolder(binding.root) {
        private var progressJob: Job? = null
        private fun resolveDownloader(): Downloader? {
            val ctx = (binding.root.context as? ViewComponentManager.FragmentContextWrapper)?.applicationContext
                ?: binding.root.context.applicationContext
            return try {
                val entry = dagger.hilt.android.EntryPointAccessors.fromApplication(ctx, DownloaderEntryPoint::class.java)
                entry.downloader()
            } catch (_: Exception) {
                null
            }
        }
        fun bind(item: FindroidItem, fixedWidth: Boolean) {
            android.util.Log.d("DownloadsUI", "Bind item ${item.id} name=${item.name} itemView size BEFORE: w=${itemView.width} h=${itemView.height}")
            itemView.post {
                android.util.Log.d("DownloadsUI", "Bind item ${item.id} itemView size AFTER LAYOUT: w=${itemView.width} h=${itemView.height}")
            }
            Timber.tag("DownloadsUI").d("Bind item %s downloaded=%s downloading=%s", item.id, item.isDownloaded(), item.isDownloading())
            binding.itemName.text = if (item is FindroidEpisode) item.seriesName else item.name
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

            // Progress badge for items currently downloading
            binding.downloadProgress.isVisible = false
            progressJob?.cancel()
            if (item.isDownloading()) {
                val downloader = resolveDownloader()
                if (downloader != null) {
                    binding.downloadProgress.isVisible = true
                    progressJob = CoroutineScope(Dispatchers.Main).launch {
                        while (isActive) {
                            // Find any local source with a downloadId and query progress
                            val source = item.sources.firstOrNull { it.downloadId != null }
                            val (status, progress) = downloader.getProgress(source?.downloadId)
                            if (progress >= 0) {
                                binding.downloadProgress.text = "$progress%"
                            }
                            // Hide badge when finished
                            if (status == android.app.DownloadManager.STATUS_SUCCESSFUL) {
                                binding.downloadProgress.isVisible = false
                                break
                            }
                            delay(1000)
                        }
                    }
                }
            }

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

    override fun getItemCount(): Int {
        val count = super.getItemCount()
        Timber.tag("DownloadsUI").d("ViewItemListAdapter.getItemCount() = %d", count)
        return count
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = getItem(position)
        Timber.tag("DownloadsUI").d("onBind item position=%d id=%s", position, item.id)
        holder.itemView.setOnClickListener {
            Timber.tag("DownloadsUI").d("click item id=%s", item.id)
            onClickListener(item)
        }
        holder.itemView.setOnLongClickListener {
            Timber.tag("DownloadsUI").d("long-click item id=%s", item.id)
            onLongClickListener?.invoke(item)
            onLongClickListener != null
        }
        // Overflow action forwards to the same delete/cancel flow
        holder.itemView.findViewById<View>(dev.jdtech.jellyfin.R.id.item_overflow)?.setOnClickListener {
            Timber.tag("DownloadsUI").d("overflow-click item id=%s", item.id)
            onLongClickListener?.invoke(item)
        }
        holder.bind(item, fixedWidth)
    }
}
