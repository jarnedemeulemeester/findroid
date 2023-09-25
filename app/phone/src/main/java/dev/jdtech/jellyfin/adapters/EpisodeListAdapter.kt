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
import dev.jdtech.jellyfin.bindItemBackdropById
import dev.jdtech.jellyfin.bindSeasonPoster
import dev.jdtech.jellyfin.databinding.EpisodeItemBinding
import dev.jdtech.jellyfin.databinding.SeasonHeaderBinding
import dev.jdtech.jellyfin.models.EpisodeItem
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.isDownloaded
import dev.jdtech.jellyfin.core.R as CoreR

private const val ITEM_VIEW_TYPE_HEADER = 0
private const val ITEM_VIEW_TYPE_EPISODE = 1

class EpisodeListAdapter(
    private val onClickListener: (item: FindroidEpisode) -> Unit,
) :
    ListAdapter<EpisodeItem, RecyclerView.ViewHolder>(DiffCallback) {

    class HeaderViewHolder(private var binding: SeasonHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(header: EpisodeItem.Header) {
            binding.seasonName.text = header.seasonName
            binding.seriesName.text = header.seriesName
            bindItemBackdropById(binding.itemBanner, header.seriesId)
            bindSeasonPoster(binding.seasonPoster, header.seasonId)
        }
    }

    class EpisodeViewHolder(private var binding: EpisodeItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(episode: FindroidEpisode) {
            binding.episodeTitle.text = if (episode.indexNumberEnd == null) {
                binding.root.context.getString(CoreR.string.episode_name, episode.indexNumber, episode.name)
            } else {
                binding.root.context.getString(CoreR.string.episode_name_with_end, episode.indexNumber, episode.indexNumberEnd, episode.name)
            }

            binding.episodeOverview.text = episode.overview

            if (episode.playbackPositionTicks > 0) {
                binding.progressBar.layoutParams.width = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    (episode.playbackPositionTicks.div(episode.runtimeTicks.toFloat()).times(84)),
                    binding.progressBar.context.resources.displayMetrics,
                ).toInt()
                binding.progressBar.visibility = View.VISIBLE
            } else {
                binding.progressBar.visibility = View.GONE
            }

            binding.playedIcon.isVisible = episode.played
            binding.missingIcon.isVisible = episode.missing
            binding.downloadedIcon.isVisible = episode.isDownloaded()

            bindCardItemImage(binding.episodeImage, episode)
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<EpisodeItem>() {
        override fun areItemsTheSame(oldItem: EpisodeItem, newItem: EpisodeItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: EpisodeItem, newItem: EpisodeItem): Boolean {
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
                        false,
                    ),
                )
            }
            ITEM_VIEW_TYPE_EPISODE -> {
                EpisodeViewHolder(
                    EpisodeItemBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false,
                    ),
                )
            }
            else -> throw ClassCastException("Unknown viewType $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            ITEM_VIEW_TYPE_HEADER -> {
                val item = getItem(position) as EpisodeItem.Header
                (holder as HeaderViewHolder).bind(item)
            }
            ITEM_VIEW_TYPE_EPISODE -> {
                val item = getItem(position) as EpisodeItem.Episode
                holder.itemView.setOnClickListener {
                    onClickListener(item.episode)
                }
                (holder as EpisodeViewHolder).bind(item.episode)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is EpisodeItem.Header -> ITEM_VIEW_TYPE_HEADER
            is EpisodeItem.Episode -> ITEM_VIEW_TYPE_EPISODE
        }
    }
}
