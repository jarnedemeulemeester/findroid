package dev.jdtech.jellyfin.adapters

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.jdtech.jellyfin.databinding.HomeEpisodeItemBinding
import org.jellyfin.sdk.model.api.BaseItemDto

class HomeEpisodeListAdapter(private val onClickListener: OnClickListener) : ListAdapter<BaseItemDto, HomeEpisodeListAdapter.EpisodeViewHolder>(DiffCallback) {
    class EpisodeViewHolder(private var binding: HomeEpisodeItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(episode: BaseItemDto) {
            binding.episode = episode
            if (episode.userData?.playedPercentage != null) {
                binding.progressBar.layoutParams.width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    (episode.userData?.playedPercentage?.times(2.24))!!.toFloat(), binding.progressBar.context.resources.displayMetrics).toInt()
                binding.progressBar.visibility = View.VISIBLE
            }

            if (episode.type == "Movie") {
                binding.primaryName.text = episode.name
                binding.secondaryName.visibility = View.GONE
            } else if (episode.type == "Episode") {
                binding.primaryName.text = episode.seriesName
            }

            binding.executePendingBindings()
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<BaseItemDto>() {
        override fun areItemsTheSame(oldItem: BaseItemDto, newItem: BaseItemDto): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: BaseItemDto, newItem: BaseItemDto): Boolean {
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

    class OnClickListener(val clickListener: (item: BaseItemDto) -> Unit) {
        fun onClick(item: BaseItemDto) = clickListener(item)
    }
}