package dev.jdtech.jellyfin.adapters

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.databinding.HomeEpisodeItemBinding
import dev.jdtech.jellyfin.models.JellyfinEpisodeItem
import dev.jdtech.jellyfin.models.JellyfinItem
import dev.jdtech.jellyfin.models.JellyfinMovieItem
import dev.jdtech.jellyfin.models.isDownloaded

class HomeEpisodeListAdapter(private val onClickListener: OnClickListener) : ListAdapter<JellyfinItem, HomeEpisodeListAdapter.EpisodeViewHolder>(DiffCallback) {
    class EpisodeViewHolder(
        private var binding: HomeEpisodeItemBinding,
        private val parent: ViewGroup
    ) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: JellyfinItem) {
            binding.item = item
            if (item.playedPercentage != null) {
                binding.progressBar.layoutParams.width = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    (item.playedPercentage?.times(2.24))!!.toFloat(), binding.progressBar.context.resources.displayMetrics
                ).toInt()
                binding.progressBar.visibility = View.VISIBLE
            }

            binding.downloadedIcon.isVisible = item.isDownloaded()

            when (item) {
                is JellyfinMovieItem -> {
                    binding.primaryName.text = item.name
                    binding.secondaryName.visibility = View.GONE
                }
                is JellyfinEpisodeItem -> {
                    binding.primaryName.text = item.seriesName
                    binding.secondaryName.text = parent.resources.getString(R.string.episode_name_extended, item.parentIndexNumber, item.indexNumber, item.name)
                }
            }

            binding.executePendingBindings()
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<JellyfinItem>() {
        override fun areItemsTheSame(oldItem: JellyfinItem, newItem: JellyfinItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: JellyfinItem, newItem: JellyfinItem): Boolean {
            return oldItem.name == newItem.name
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpisodeViewHolder {
        return EpisodeViewHolder(
            HomeEpisodeItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ),
            parent
        )
    }

    override fun onBindViewHolder(holder: EpisodeViewHolder, position: Int) {
        val item = getItem(position)
        holder.itemView.setOnClickListener {
            onClickListener.onClick(item)
        }
        holder.bind(item)
    }

    class OnClickListener(val clickListener: (item: JellyfinItem) -> Unit) {
        fun onClick(item: JellyfinItem) = clickListener(item)
    }
}
