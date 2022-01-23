package dev.jdtech.jellyfin.adapters

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.jdtech.jellyfin.databinding.HomeEpisodeItemBinding
import dev.jdtech.jellyfin.models.ContentType
import dev.jdtech.jellyfin.models.PlayerItem
import dev.jdtech.jellyfin.utils.downloadMetadataToBaseItemDto
import timber.log.Timber

class DownloadEpisodeListAdapter(private val onClickListener: OnClickListener) : ListAdapter<PlayerItem, DownloadEpisodeListAdapter.EpisodeViewHolder>(DiffCallback) {
    class EpisodeViewHolder(private var binding: HomeEpisodeItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(episode: PlayerItem) {
            val metadata = episode.item!!
            binding.episode = downloadMetadataToBaseItemDto(episode.item)
            if (metadata.playedPercentage != null) {
                binding.progressBar.layoutParams.width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    (metadata.playedPercentage.times(2.24)).toFloat(), binding.progressBar.context.resources.displayMetrics).toInt()
                binding.progressBar.visibility = View.VISIBLE
            }
            if (metadata.type == ContentType.MOVIE) {
                binding.primaryName.text = metadata.name
                Timber.d(metadata.name)
                binding.secondaryName.visibility = View.GONE
            } else if (metadata.type == ContentType.EPISODE) {
                binding.primaryName.text = metadata.seriesName
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpisodeViewHolder {
        return EpisodeViewHolder(
            HomeEpisodeItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: EpisodeViewHolder, position: Int) {
        val item = getItem(position)
        holder.itemView.setOnClickListener {
            onClickListener.onClick(item)
        }
        holder.bind(item)
    }

    class OnClickListener(val clickListener: (item: PlayerItem) -> Unit) {
        fun onClick(item: PlayerItem) = clickListener(item)
    }
}