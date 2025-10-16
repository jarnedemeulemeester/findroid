package dev.jdtech.jellyfin.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import dev.jdtech.jellyfin.R

data class GenreItem(val name: String, val isSelected: Boolean)

class GenresAdapter(
    private val onGenreClick: (String?) -> Unit,
) : ListAdapter<GenreItem, GenresAdapter.GenreViewHolder>(DiffCallback) {

    class GenreViewHolder(
        private val chip: Chip,
        private val onGenreClick: (String?) -> Unit,
    ) : RecyclerView.ViewHolder(chip) {
        fun bind(genre: GenreItem) {
            chip.text = genre.name
            chip.isChecked = genre.isSelected
            chip.setOnClickListener {
                onGenreClick(if (genre.name == "Todos") null else genre.name)
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<GenreItem>() {
        override fun areItemsTheSame(oldItem: GenreItem, newItem: GenreItem): Boolean {
            return oldItem.name == newItem.name
        }

        override fun areContentsTheSame(oldItem: GenreItem, newItem: GenreItem): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GenreViewHolder {
        val chip = LayoutInflater.from(parent.context)
            .inflate(R.layout.genre_chip, parent, false) as Chip
        return GenreViewHolder(chip, onGenreClick)
    }

    override fun onBindViewHolder(holder: GenreViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
