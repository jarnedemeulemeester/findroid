package dev.jdtech.jellyfin.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.jdtech.jellyfin.core.Constants
import androidx.recyclerview.widget.LinearLayoutManager
import dev.jdtech.jellyfin.databinding.FavoriteSectionBinding
import dev.jdtech.jellyfin.models.CollectionSection
import dev.jdtech.jellyfin.models.FindroidItem
import timber.log.Timber

class FavoritesListAdapter(
    private val onItemClickListener: (item: FindroidItem) -> Unit,
    private val onItemLongClickListener: ((item: FindroidItem) -> Unit)? = null,
    private val onGenreClickListener: ((String?) -> Unit)? = null,
) : ListAdapter<CollectionSection, FavoritesListAdapter.SectionViewHolder>(DiffCallback) {
    
    private var genres: List<String> = emptyList()
    private var selectedGenre: String? = null
    
    fun updateGenres(genres: List<String>, selectedGenre: String?) {
        this.genres = genres
        this.selectedGenre = selectedGenre
        notifyDataSetChanged()
    }
    
    class SectionViewHolder(private var binding: FavoriteSectionBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(
            section: CollectionSection,
            onItemClickListener: (item: FindroidItem) -> Unit,
            onItemLongClickListener: ((item: FindroidItem) -> Unit)?,
            genres: List<String>,
            selectedGenre: String?,
            onGenreClickListener: ((String?) -> Unit)?,
        ) {
            Timber.tag("DownloadsUI").d("Bind section id=%d size=%d", section.id, section.items.size)
            // Ensure a horizontal layout manager and proper measuring
            if (binding.itemsRecyclerView.layoutManager !is LinearLayoutManager ||
                (binding.itemsRecyclerView.layoutManager as? LinearLayoutManager)?.orientation != RecyclerView.HORIZONTAL
            ) {
                binding.itemsRecyclerView.layoutManager = LinearLayoutManager(binding.root.context, RecyclerView.HORIZONTAL, false)
                binding.itemsRecyclerView.isNestedScrollingEnabled = false
                binding.itemsRecyclerView.itemAnimator = null
            }
            if (section.id == Constants.FAVORITE_TYPE_MOVIES || section.id == Constants.FAVORITE_TYPE_SHOWS) {
                binding.itemsRecyclerView.adapter =
                    ViewItemListAdapter(onItemClickListener, fixedWidth = true, onLongClickListener = onItemLongClickListener)
                (binding.itemsRecyclerView.adapter as ViewItemListAdapter).submitList(section.items)
                Timber.tag("DownloadsUI").d("Submit items to ViewItemListAdapter size=%d", section.items.size)
                binding.itemsRecyclerView.requestLayout()
            } else if (section.id == Constants.FAVORITE_TYPE_EPISODES) {
                binding.itemsRecyclerView.adapter =
                    HomeEpisodeListAdapter(onItemClickListener, onItemLongClickListener)
                (binding.itemsRecyclerView.adapter as HomeEpisodeListAdapter).submitList(section.items)
                Timber.tag("DownloadsUI").d("Submit items to HomeEpisodeListAdapter size=%d", section.items.size)
                binding.itemsRecyclerView.requestLayout()
            }
            binding.sectionName.text = section.name.asString(binding.root.resources)
            
            // Setup genres RecyclerView (only for Movies and Shows sections)
            if ((section.id == Constants.FAVORITE_TYPE_MOVIES || section.id == Constants.FAVORITE_TYPE_SHOWS) && genres.isNotEmpty()) {
                binding.genresRecyclerView.visibility = android.view.View.VISIBLE
                binding.genresRecyclerView.layoutManager = LinearLayoutManager(binding.root.context, RecyclerView.HORIZONTAL, false)
                
                val genreItems = mutableListOf(GenreItem("Todos", selectedGenre == null))
                genreItems.addAll(genres.map { GenreItem(it, it == selectedGenre) })
                
                val genresAdapter = GenresAdapter(onGenreClickListener ?: {})
                binding.genresRecyclerView.adapter = genresAdapter
                genresAdapter.submitList(genreItems)
            } else {
                binding.genresRecyclerView.visibility = android.view.View.GONE
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<CollectionSection>() {
        override fun areItemsTheSame(oldItem: CollectionSection, newItem: CollectionSection): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: CollectionSection,
            newItem: CollectionSection,
        ): Boolean {
            return oldItem == newItem
        }
    }

    override fun getItemCount(): Int {
        val count = super.getItemCount()
        android.util.Log.d("DownloadsUI", "FavoritesListAdapter.getItemCount() = $count")
        Timber.tag("DownloadsUI").d("FavoritesListAdapter.getItemCount() = %d", count)
        return count
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionViewHolder {
        android.util.Log.d("DownloadsUI", "========== FavoritesListAdapter.onCreateViewHolder CALLED ==========")
        Timber.tag("DownloadsUI").d("FavoritesListAdapter.onCreateViewHolder called")
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
        android.util.Log.d("DownloadsUI", "========== onBindViewHolder section position=$position id=${collection.id} items=${collection.items.size} ==========")
        Timber.tag("DownloadsUI").d("onBind section position=%d id=%d items=%d", position, collection.id, collection.items.size)
        holder.bind(collection, onItemClickListener, onItemLongClickListener, genres, selectedGenre, onGenreClickListener)
    }
}
