package dev.jdtech.jellyfin.adapters

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.internal.managers.ViewComponentManager
import dev.jdtech.jellyfin.bindCardItemImage
import dev.jdtech.jellyfin.databinding.HomeEpisodeItemBinding
import dev.jdtech.jellyfin.models.JellyCastEpisode
import dev.jdtech.jellyfin.models.JellyCastItem
import dev.jdtech.jellyfin.models.JellyCastMovie
import dev.jdtech.jellyfin.models.isDownloaded
import dev.jdtech.jellyfin.models.isDownloading
import dev.jdtech.jellyfin.presentation.downloads.DownloaderEntryPoint
import dev.jdtech.jellyfin.utils.Downloader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import dev.jdtech.jellyfin.core.R as CoreR
import timber.log.Timber

class HomeEpisodeListAdapter(
    private val onClickListener: (item: JellyCastItem) -> Unit,
    private val onLongClickListener: ((item: JellyCastItem) -> Unit)? = null,
) : ListAdapter<JellyCastItem, HomeEpisodeListAdapter.EpisodeViewHolder>(DiffCallback) {
    class EpisodeViewHolder(
        private var binding: HomeEpisodeItemBinding,
        private val parent: ViewGroup,
    ) :
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
        
        fun bind(item: JellyCastItem) {
            android.util.Log.d("DownloadsUI", "Bind episode ${item.id} name=${item.name} itemView size: w=${itemView.width} h=${itemView.height}")
            Timber.tag("DownloadsUI").d("Bind episode item %s downloaded=%s downloading=%s", item.id, item.isDownloaded(), item.isDownloading())
            if (item.playbackPositionTicks > 0) {
                binding.progressBar.layoutParams.width = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    (item.playbackPositionTicks.div(item.runtimeTicks.toFloat()).times(224)),
                    binding.progressBar.context.resources.displayMetrics,
                ).toInt()
                binding.progressBar.visibility = View.VISIBLE
            }

            binding.downloadedIcon.isVisible = item.isDownloaded()
            
            // Cancel previous progress job
            progressJob?.cancel()
            
            // Apply blur and show progress for items currently downloading
            if (item.isDownloading()) {
                Timber.tag("DownloadsUI").d("Episode is downloading, showing circular progress UI")
                
                // Apply blur effect to image (API 31+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    binding.episodeImage.setRenderEffect(
                        RenderEffect.createBlurEffect(15f, 15f, Shader.TileMode.CLAMP)
                    )
                }
                
                // Show overlay and circular progress
                binding.downloadBlurOverlay.isVisible = true
                binding.circularProgress.isVisible = true
                binding.circularProgressText.isVisible = true
                binding.circularProgress.max = 100
                binding.circularProgress.progress = 0
                
                val downloader = resolveDownloader()
                if (downloader != null) {
                    progressJob = CoroutineScope(Dispatchers.Main).launch {
                        while (isActive) {
                            // Find any local source with a downloadId and query progress
                            val source = item.sources.firstOrNull { it.downloadId != null }
                            val (status, progress) = downloader.getProgress(source?.downloadId)
                            Timber.tag("DownloadsUI").d("Episode download progress: %d%% (status=%d)", progress, status)
                            if (progress >= 0) {
                                binding.circularProgress.setProgressCompat(progress, true)
                                binding.circularProgressText.text = "$progress%"
                            }
                            // Hide blur and progress when finished
                            if (status == android.app.DownloadManager.STATUS_SUCCESSFUL) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    binding.episodeImage.setRenderEffect(null)
                                }
                                binding.downloadBlurOverlay.isVisible = false
                                binding.circularProgress.isVisible = false
                                binding.circularProgressText.isVisible = false
                                Timber.tag("DownloadsUI").d("Episode download completed, hiding progress UI")
                                break
                            }
                            delay(1000)
                        }
                    }
                } else {
                    Timber.tag("DownloadsUI").w("Downloader is null for episode, cannot show progress")
                }
            } else {
                // Remove blur for downloaded or normal items
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    binding.episodeImage.setRenderEffect(null)
                }
                binding.downloadBlurOverlay.isVisible = false
                binding.circularProgress.isVisible = false
                binding.circularProgressText.isVisible = false
            }

            when (item) {
                is JellyCastMovie -> {
                    binding.primaryName.text = item.name
                    binding.secondaryName.visibility = View.GONE
                }
                is JellyCastEpisode -> {
                    binding.primaryName.text = item.seriesName
                    binding.secondaryName.text = if (item.indexNumberEnd == null) {
                        parent.resources.getString(CoreR.string.episode_name_extended, item.parentIndexNumber, item.indexNumber, item.name)
                    } else {
                        parent.resources.getString(CoreR.string.episode_name_extended_with_end, item.parentIndexNumber, item.indexNumber, item.indexNumberEnd, item.name)
                    }
                }
            }

            bindCardItemImage(binding.episodeImage, item)
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpisodeViewHolder {
        return EpisodeViewHolder(
            HomeEpisodeItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            ),
            parent,
        )
    }

    override fun getItemCount(): Int {
        val count = super.getItemCount()
        Timber.tag("DownloadsUI").d("HomeEpisodeListAdapter.getItemCount() = %d", count)
        return count
    }

    override fun onBindViewHolder(holder: EpisodeViewHolder, position: Int) {
        val item = getItem(position)
        Timber.tag("DownloadsUI").d("onBind episode position=%d id=%s", position, item.id)
        holder.itemView.setOnClickListener {
            Timber.tag("DownloadsUI").d("click episode id=%s", item.id)
            onClickListener(item)
        }
        holder.itemView.setOnLongClickListener {
            Timber.tag("DownloadsUI").d("long-click episode id=%s", item.id)
            onLongClickListener?.invoke(item)
            onLongClickListener != null
        }
        holder.itemView.findViewById<View>(dev.jdtech.jellyfin.R.id.item_overflow)?.setOnClickListener {
            Timber.tag("DownloadsUI").d("overflow-click episode id=%s", item.id)
            onLongClickListener?.invoke(item)
        }
        holder.bind(item)
    }
}
