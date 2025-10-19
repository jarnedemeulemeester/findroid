package dev.jdtech.jellyfin.adapters

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
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
import dev.jdtech.jellyfin.models.JellyCastEpisode
import dev.jdtech.jellyfin.models.JellyCastItem
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
    private val onClickListener: (item: JellyCastItem) -> Unit,
    private val fixedWidth: Boolean = false,
    private val onLongClickListener: ((item: JellyCastItem) -> Unit)? = null,
) : ListAdapter<JellyCastItem, ViewItemListAdapter.ItemViewHolder>(DiffCallback) {

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
        fun bind(item: JellyCastItem, fixedWidth: Boolean) {
            android.util.Log.d("DownloadsUI", "Bind item ${item.id} name=${item.name} itemView size BEFORE: w=${itemView.width} h=${itemView.height}")
            itemView.post {
                android.util.Log.d("DownloadsUI", "Bind item ${item.id} itemView size AFTER LAYOUT: w=${itemView.width} h=${itemView.height}")
            }
            Timber.tag("DownloadsUI").d("Bind item %s downloaded=%s downloading=%s", item.id, item.isDownloaded(), item.isDownloading())
            
            // Log sources for debugging
            item.sources.forEachIndexed { index, source ->
                Timber.tag("DownloadsUI").d("  Source %d: type=%s path=%s downloadId=%s", 
                    index, source.type, source.path, source.downloadId)
            }
            
            binding.itemName.text = if (item is JellyCastEpisode) item.seriesName else item.name
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

            // Hide old progress badge
            binding.downloadProgress.isVisible = false
            progressJob?.cancel()
            
            // Apply blur and show progress for items currently downloading
            if (item.isDownloading()) {
                Timber.tag("DownloadsUI").d("Item is downloading, showing circular progress UI")
                
                // Apply blur effect to image (API 31+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    binding.itemImage.setRenderEffect(
                        RenderEffect.createBlurEffect(15f, 15f, Shader.TileMode.CLAMP)
                    )
                }
                
                // Show overlay and circular progress
                binding.downloadBlurOverlay.isVisible = true
                binding.circularProgress.isVisible = true
                binding.circularProgressText.isVisible = true
                binding.circularProgress.max = 100
                binding.circularProgress.progress = 0
                
                // Hide text progress below title
                try {
                    binding.downloadProgressText.isVisible = false
                } catch (e: Exception) {
                    // Ignore
                }
                
                val downloader = resolveDownloader()
                if (downloader != null) {
                    progressJob = CoroutineScope(Dispatchers.Main).launch {
                        while (isActive) {
                            // Find any local source with a downloadId and query progress
                            val source = item.sources.firstOrNull { it.downloadId != null }
                            val (status, progress) = downloader.getProgress(source?.downloadId)
                            Timber.tag("DownloadsUI").d("Download progress: %d%% (status=%d, downloadId=%s)", 
                                progress, status, source?.downloadId)
                            if (progress >= 0) {
                                binding.circularProgress.setProgressCompat(progress, true)
                                binding.circularProgressText.text = "$progress%"
                            }
                            // Hide blur and progress when finished
                            if (status == android.app.DownloadManager.STATUS_SUCCESSFUL) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    binding.itemImage.setRenderEffect(null)
                                }
                                binding.downloadBlurOverlay.isVisible = false
                                binding.circularProgress.isVisible = false
                                binding.circularProgressText.isVisible = false
                                Timber.tag("DownloadsUI").d("Download completed, hiding progress UI")
                                break
                            }
                            delay(1000)
                        }
                    }
                } else {
                    Timber.tag("DownloadsUI").w("Downloader is null, cannot show progress")
                }
            } else {
                // Remove blur for downloaded or normal items
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    binding.itemImage.setRenderEffect(null)
                }
                binding.downloadBlurOverlay.isVisible = false
                binding.circularProgress.isVisible = false
                binding.circularProgressText.isVisible = false
                try {
                    binding.downloadProgressText.isVisible = false
                } catch (e: Exception) {
                    // Ignore if binding doesn't have this field yet
                }
            }

            bindItemImage(binding.itemImage, item)
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<JellyCastItem>() {
        override fun areItemsTheSame(oldItem: JellyCastItem, newItem: JellyCastItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: JellyCastItem, newItem: JellyCastItem): Boolean {
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
