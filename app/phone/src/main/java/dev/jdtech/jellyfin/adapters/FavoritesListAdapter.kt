package dev.jdtech.jellyfin.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.jdtech.jellyfin.Constants
import dev.jdtech.jellyfin.databinding.FavoriteSectionBinding
import dev.jdtech.jellyfin.models.FavoriteSection
import dev.jdtech.jellyfin.models.FindroidItem

class FavoritesListAdapter(
    private val onItemClickListener: (item: FindroidItem) -> Unit,
) : ListAdapter<FavoriteSection, FavoritesListAdapter.SectionViewHolder>(DiffCallback) {
    class SectionViewHolder(private var binding: FavoriteSectionBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(
            section: FavoriteSection,
            onItemClickListener: (item: FindroidItem) -> Unit,
        ) {
            if (section.id == Constants.FAVORITE_TYPE_MOVIES || section.id == Constants.FAVORITE_TYPE_SHOWS) {
                binding.itemsRecyclerView.adapter =
                    ViewItemListAdapter(onItemClickListener, fixedWidth = true)
                (binding.itemsRecyclerView.adapter as ViewItemListAdapter).submitList(section.items)
            } else if (section.id == Constants.FAVORITE_TYPE_EPISODES) {
                binding.itemsRecyclerView.adapter =
                    HomeEpisodeListAdapter(onItemClickListener)
                (binding.itemsRecyclerView.adapter as HomeEpisodeListAdapter).submitList(section.items)
            }
            binding.sectionName.text = section.name.asString(binding.root.resources)
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<FavoriteSection>() {
        override fun areItemsTheSame(oldItem: FavoriteSection, newItem: FavoriteSection): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: FavoriteSection,
            newItem: FavoriteSection,
        ): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionViewHolder {
        return SectionViewHolder(
            FavoriteSectionBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            ),
        )
    }

    override fun onBindViewHolder(holder: SectionViewHolder, position: Int) {
        val collection = getItem(position)
        holder.bind(collection, onItemClickListener)
    }
}
