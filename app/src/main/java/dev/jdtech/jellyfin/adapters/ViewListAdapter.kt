package dev.jdtech.jellyfin.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.databinding.NextUpSectionBinding
import dev.jdtech.jellyfin.databinding.ViewItemBinding
import dev.jdtech.jellyfin.models.HomeSection
import dev.jdtech.jellyfin.models.View
import java.util.UUID

private const val ITEM_VIEW_TYPE_NEXT_UP = 0
private const val ITEM_VIEW_TYPE_VIEW = 1

class ViewListAdapter(
    private val onClickListener: OnClickListener,
    private val onItemClickListener: ViewItemListAdapter.OnClickListener,
    private val onNextUpClickListener: HomeEpisodeListAdapter.OnClickListener
) : ListAdapter<HomeItem, RecyclerView.ViewHolder>(DiffCallback) {

    class ViewViewHolder(private var binding: ViewItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(
            dataItem: HomeItem.ViewItem,
            onClickListener: OnClickListener,
            onItemClickListener: ViewItemListAdapter.OnClickListener
        ) {
            val view = dataItem.view
            binding.view = view
            binding.viewName.text = String.format(binding.viewName.context.resources.getString(R.string.latest_library), view.name)
            binding.itemsRecyclerView.adapter =
                ViewItemListAdapter(onItemClickListener, fixedWidth = true)
            binding.viewAll.setOnClickListener {
                onClickListener.onClick(view)
            }
            binding.executePendingBindings()
        }
    }

    class NextUpViewHolder(private var binding: NextUpSectionBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(section: HomeItem.Section, onClickListener: HomeEpisodeListAdapter.OnClickListener) {
            binding.section = section.homeSection
            binding.itemsRecyclerView.adapter = HomeEpisodeListAdapter(onClickListener)
            binding.executePendingBindings()
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<HomeItem>() {
        override fun areItemsTheSame(oldItem: HomeItem, newItem: HomeItem): Boolean {
            return oldItem.ids.size == newItem.ids.size
                && oldItem.ids.mapIndexed { i, old -> old == newItem.ids[i] }.all { it }
        }

        override fun areContentsTheSame(oldItem: HomeItem, newItem: HomeItem): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ITEM_VIEW_TYPE_NEXT_UP -> NextUpViewHolder(
                NextUpSectionBinding.inflate(
                    LayoutInflater.from(
                        parent.context
                    ), parent, false
                )
            )
            ITEM_VIEW_TYPE_VIEW -> ViewViewHolder(
                ViewItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
            else -> throw ClassCastException("Unknown viewType $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            ITEM_VIEW_TYPE_NEXT_UP -> {
                val view = getItem(position) as HomeItem.Section
                (holder as NextUpViewHolder).bind(view, onNextUpClickListener)
            }
            ITEM_VIEW_TYPE_VIEW -> {
                val view = getItem(position) as HomeItem.ViewItem
                (holder as ViewViewHolder).bind(view, onClickListener, onItemClickListener)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is HomeItem.Section -> ITEM_VIEW_TYPE_NEXT_UP
            is HomeItem.ViewItem -> ITEM_VIEW_TYPE_VIEW
        }
    }

    class OnClickListener(val clickListener: (view: View) -> Unit) {
        fun onClick(view: View) = clickListener(view)
    }
}

sealed class HomeItem {
    data class Section(val homeSection: HomeSection) : HomeItem() {
        override val ids = homeSection.items.map { it.id }
    }

    data class ViewItem(val view: View) : HomeItem() {
        override val ids = view.items?.map { it.id }.orEmpty()
    }

    abstract val ids: List<UUID>
}