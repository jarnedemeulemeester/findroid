package dev.jdtech.jellyfin.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.jdtech.jellyfin.databinding.ServerItemBinding
import dev.jdtech.jellyfin.models.Server

class ServerGridAdapter(
    private val onClickListener: (server: Server) -> Unit,
    private val onLongClickListener: (server: Server) -> Boolean,
) : ListAdapter<Server, ServerGridAdapter.ServerViewHolder>(DiffCallback) {
    class ServerViewHolder(private var binding: ServerItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(server: Server) {
            binding.serverName.text = server.name
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Server>() {
        override fun areItemsTheSame(oldItem: Server, newItem: Server): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Server, newItem: Server): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ServerViewHolder {
        return ServerViewHolder(ServerItemBinding.inflate(LayoutInflater.from(parent.context)))
    }

    override fun onBindViewHolder(holder: ServerViewHolder, position: Int) {
        val server = getItem(position)
        holder.itemView.setOnClickListener {
            onClickListener(server)
        }
        holder.itemView.setOnLongClickListener {
            onLongClickListener(server)
        }
        holder.bind(server)
    }
}
