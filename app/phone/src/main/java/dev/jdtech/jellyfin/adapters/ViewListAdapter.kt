package dev.jdtech.jellyfin.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.jdtech.jellyfin.databinding.CardOfflineBinding
import dev.jdtech.jellyfin.databinding.NextUpSectionBinding
import dev.jdtech.jellyfin.databinding.ViewItemBinding
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.HomeItem
import dev.jdtech.jellyfin.models.View
import dev.jdtech.jellyfin.core.R as CoreR

private const val ITEM_VIEW_TYPE_NEXT_UP = 0
private const val ITEM_VIEW_TYPE_VIEW = 1
private const val ITEM_VIEW_TYPE_OFFLINE_CARD = 2

class ViewListAdapter(
    private val onClickListener: (view: View) -> Unit,
    private val onItemClickListener: (item: FindroidItem) -> Unit,
    private val onOnlineClickListener: () -> Unit,
) : ListAdapter<HomeItem, RecyclerView.ViewHolder>(DiffCallback) {

    class ViewViewHolder(private var binding: ViewItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(
            dataItem: HomeItem.ViewItem,
            onClickListener: (view: View) -> Unit,
            onItemClickListener: (item: FindroidItem) -> Unit,
        ) {
            val view = dataItem.view
            binding.viewName.text = binding.viewName.context.resources.getString(CoreR.string.latest_library, view.name)
            binding.itemsRecyclerView.adapter =
                ViewItemListAdapter(onItemClickListener, fixedWidth = true)
            (binding.itemsRecyclerView.adapter as ViewItemListAdapter).submitList(view.items)
            binding.viewAll.setOnClickListener {
                onClickListener(view)
            }
        }
    }

    class NextUpViewHolder(private var binding: NextUpSectionBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(
            section: HomeItem.Section,
            onClickListener: (item: FindroidItem) -> Unit,
        ) {
            binding.sectionName.text = section.homeSection.name.asString(binding.sectionName.context.resources)
            binding.itemsRecyclerView.adapter = HomeEpisodeListAdapter(onClickListener)
            (binding.itemsRecyclerView.adapter as HomeEpisodeListAdapter).submitList(section.homeSection.items)
        }
    }

    class OfflineCardViewHolder(private var binding: CardOfflineBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(onClickListener: () -> Unit) {
            binding.onlineButton.setOnClickListener {
                onClickListener()
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<HomeItem>() {
        override fun areItemsTheSame(oldItem: HomeItem, newItem: HomeItem): Boolean {
            return oldItem.id == newItem.id
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
                        parent.context,
                    ),
                    parent,
                    false,
                ),
            )
            ITEM_VIEW_TYPE_VIEW -> ViewViewHolder(
                ViewItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                ),
            )
            ITEM_VIEW_TYPE_OFFLINE_CARD -> {
                OfflineCardViewHolder(
                    CardOfflineBinding.inflate(
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
            ITEM_VIEW_TYPE_NEXT_UP -> {
                val view = getItem(position) as HomeItem.Section
                (holder as NextUpViewHolder).bind(view, onItemClickListener)
            }
            ITEM_VIEW_TYPE_VIEW -> {
                val view = getItem(position) as HomeItem.ViewItem
                (holder as ViewViewHolder).bind(view, onClickListener, onItemClickListener)
            }
            ITEM_VIEW_TYPE_OFFLINE_CARD -> {
                (holder as OfflineCardViewHolder).bind(onOnlineClickListener)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is HomeItem.OfflineCard -> ITEM_VIEW_TYPE_OFFLINE_CARD
            is HomeItem.Libraries -> -1
            is HomeItem.Section -> ITEM_VIEW_TYPE_NEXT_UP
            is HomeItem.ViewItem -> ITEM_VIEW_TYPE_VIEW
        }
    }
}
