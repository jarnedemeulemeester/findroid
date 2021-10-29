package dev.jdtech.jellyfin.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.jdtech.jellyfin.databinding.DownloadSectionBinding
import dev.jdtech.jellyfin.models.DownloadSection

class DownloadsListAdapter(
    private val onClickListener: DownloadViewItemListAdapter.OnClickListener,
    private val onEpisodeClickListener: DownloadEpisodeListAdapter.OnClickListener
) : ListAdapter<DownloadSection, DownloadsListAdapter.SectionViewHolder>(DiffCallback) {
    class SectionViewHolder(private var binding: DownloadSectionBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(
            section: DownloadSection,
            onClickListener: DownloadViewItemListAdapter.OnClickListener,
            onEpisodeClickListener: DownloadEpisodeListAdapter.OnClickListener
        ) {
            binding.section = section
            if (section.name == "Movies" || section.name == "Shows") {
                binding.itemsRecyclerView.adapter =
                    DownloadViewItemListAdapter(onClickListener, fixedWidth = true)
                (binding.itemsRecyclerView.adapter as DownloadViewItemListAdapter).submitList(section.items)
            } else if (section.name == "Episodes") {
                binding.itemsRecyclerView.adapter =
                    DownloadEpisodeListAdapter(onEpisodeClickListener)
                (binding.itemsRecyclerView.adapter as DownloadEpisodeListAdapter).submitList(section.items)
            }
            binding.executePendingBindings()
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<DownloadSection>() {
        override fun areItemsTheSame(oldItem: DownloadSection, newItem: DownloadSection): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: DownloadSection,
            newItem: DownloadSection
        ): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionViewHolder {
        return SectionViewHolder(
            DownloadSectionBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: SectionViewHolder, position: Int) {
        val collection = getItem(position)
        holder.bind(collection, onClickListener, onEpisodeClickListener)
    }
}