package dev.jdtech.jellyfin.adapters

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.jdtech.jellyfin.databinding.EpisodeItemBinding
import dev.jdtech.jellyfin.databinding.SeasonHeaderBinding
import dev.jdtech.jellyfin.models.DownloadEpisodeItem
import dev.jdtech.jellyfin.models.DownloadSeriesMetadata
import dev.jdtech.jellyfin.models.PlayerItem
import dev.jdtech.jellyfin.utils.downloadMetadataToBaseItemDto
import org.jellyfin.sdk.model.api.BaseItemDto

private const val ITEM_VIEW_TYPE_HEADER = 0
private const val ITEM_VIEW_TYPE_EPISODE = 1

class DownloadEpisodeListAdapter(
    private val onClickListener: OnClickListener,
    private val downloadSeriesMetadata: dev.jdtech.jellyfin.models.DownloadSeriesMetadata
) :
    ListAdapter<DownloadEpisodeItem, RecyclerView.ViewHolder>(DiffCallback) {

    class HeaderViewHolder(private var binding: SeasonHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(
            metadata: dev.jdtech.jellyfin.models.DownloadSeriesMetadata
        ) {
            binding.seasonName.text = metadata.name
            binding.seriesId = metadata.itemId
            binding.seasonId = metadata.itemId
            binding.executePendingBindings()
        }
    }

    class EpisodeViewHolder(private var binding: EpisodeItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(episode: BaseItemDto) {
            binding.episode = episode
            if (episode.userData?.playedPercentage != null) {
                binding.progressBar.layoutParams.width = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    (episode.userData?.playedPercentage?.times(.84))!!.toFloat(),
                    binding.progressBar.context.resources.displayMetrics
                ).toInt()
                binding.progressBar.visibility = View.VISIBLE
            } else {
                binding.progressBar.visibility = View.GONE
            }
            binding.executePendingBindings()
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<DownloadEpisodeItem>() {
        override fun areItemsTheSame(oldItem: DownloadEpisodeItem, newItem: DownloadEpisodeItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: DownloadEpisodeItem, newItem: DownloadEpisodeItem): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ITEM_VIEW_TYPE_HEADER -> {
                HeaderViewHolder(
                    SeasonHeaderBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }
            ITEM_VIEW_TYPE_EPISODE -> {
                EpisodeViewHolder(
                    EpisodeItemBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }
            else -> throw ClassCastException("Unknown viewType $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            ITEM_VIEW_TYPE_HEADER -> {
                (holder as HeaderViewHolder).bind(downloadSeriesMetadata)
            }
            ITEM_VIEW_TYPE_EPISODE -> {
                val item = getItem(position) as DownloadEpisodeItem.Episode
                holder.itemView.setOnClickListener {
                    onClickListener.onClick(item.episode)
                }
                (holder as EpisodeViewHolder).bind(downloadMetadataToBaseItemDto(item.episode.item!!))
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is DownloadEpisodeItem.Header -> ITEM_VIEW_TYPE_HEADER
            is DownloadEpisodeItem.Episode -> ITEM_VIEW_TYPE_EPISODE
        }
    }

    class OnClickListener(val clickListener: (item: PlayerItem) -> Unit) {
        fun onClick(item: PlayerItem) = clickListener(item)
    }
}