package dev.jdtech.jellyfin.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.jdtech.jellyfin.databinding.ServerAddressListItemBinding
import dev.jdtech.jellyfin.models.ServerAddress

class ServerAddressAdapter(
    private val clickListener: (address: ServerAddress) -> Unit,
    private val longClickListener: (address: ServerAddress) -> Boolean,
) : ListAdapter<ServerAddress, ServerAddressAdapter.ServerAddressViewHolder>(DiffCallback) {
    class ServerAddressViewHolder(private var binding: ServerAddressListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(address: ServerAddress) {
            binding.serverAddress.text = address.address
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<ServerAddress>() {
        override fun areItemsTheSame(oldItem: ServerAddress, newItem: ServerAddress): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ServerAddress, newItem: ServerAddress): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ServerAddressViewHolder {
        return ServerAddressViewHolder(
            ServerAddressListItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            ),
        )
    }

    override fun onBindViewHolder(holder: ServerAddressViewHolder, position: Int) {
        val address = getItem(position)
        holder.itemView.setOnClickListener { clickListener(address) }
        holder.itemView.setOnLongClickListener { longClickListener(address) }
        holder.bind(address)
    }
}
