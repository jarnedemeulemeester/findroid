package dev.jdtech.jellyfin.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.jdtech.jellyfin.databinding.ViewItemBinding
import dev.jdtech.jellyfin.models.View

class ViewListAdapter(
    private val onClickListener: OnClickListener
) : ListAdapter<View, ViewListAdapter.ViewViewHolder>(DiffCallback) {
    class ViewViewHolder(private var binding: ViewItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(view: View, onClickListener: OnClickListener) {
            binding.view = view
            // TODO: Change to string placeholder
            binding.viewName.text = "Latest ${view.name}"
            binding.itemsRecyclerView.adapter = ViewItemListAdapter(fixedWidth = true)
            binding.viewAll.setOnClickListener {
                onClickListener.onClick(view)
            }
            binding.executePendingBindings()
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<View>() {
        override fun areItemsTheSame(oldItem: View, newItem: View): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: View, newItem: View): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewViewHolder {
        return ViewViewHolder(ViewItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewViewHolder, position: Int) {
        val view = getItem(position)
        holder.bind(view, onClickListener)
    }

    class OnClickListener(val clickListener: (view: View) -> Unit) {
        fun onClick(view: View) = clickListener(view)
    }
}