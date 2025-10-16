package dev.jdtech.jellyfin.adapters

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.jdtech.jellyfin.bindCardItemImage
import dev.jdtech.jellyfin.databinding.HomeEpisodeItemBinding
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.FindroidMovie
import dev.jdtech.jellyfin.models.isDownloaded
import dev.jdtech.jellyfin.core.R as CoreR
import timber.log.Timber

class HomeEpisodeListAdapter(
    private val onClickListener: (item: FindroidItem) -> Unit,
    private val onLongClickListener: ((item: FindroidItem) -> Unit)? = null,
) : ListAdapter<FindroidItem, HomeEpisodeListAdapter.EpisodeViewHolder>(DiffCallback) {
    class EpisodeViewHolder(
        private var binding: HomeEpisodeItemBinding,
        private val parent: ViewGroup,
    ) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: FindroidItem) {
            android.util.Log.d("DownloadsUI", "Bind episode ${item.id} name=${item.name} itemView size: w=${itemView.width} h=${itemView.height}")
            Timber.tag("DownloadsUI").d("Bind episode item %s downloaded=%s", item.id, item.isDownloaded())
            if (item.playbackPositionTicks > 0) {
                binding.progressBar.layoutParams.width = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    (item.playbackPositionTicks.div(item.runtimeTicks.toFloat()).times(224)),
                    binding.progressBar.context.resources.displayMetrics,
                ).toInt()
                binding.progressBar.visibility = View.VISIBLE
            }

            binding.downloadedIcon.isVisible = item.isDownloaded()

            when (item) {
                is FindroidMovie -> {
                    binding.primaryName.text = item.name
                    binding.secondaryName.visibility = View.GONE
                }
                is FindroidEpisode -> {
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

    companion object DiffCallback : DiffUtil.ItemCallback<FindroidItem>() {
        override fun areItemsTheSame(oldItem: FindroidItem, newItem: FindroidItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: FindroidItem, newItem: FindroidItem): Boolean {
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
