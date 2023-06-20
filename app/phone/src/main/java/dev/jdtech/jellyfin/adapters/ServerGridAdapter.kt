package dev.jdtech.jellyfin.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.jdtech.jellyfin.databinding.ServerItemBinding
import dev.jdtech.jellyfin.models.Server

class ServerGridAdapter(
    private val onClickListener: OnClickListener,
    private val onLongClickListener: OnLongClickListener,
) : ListAdapter<Server, ServerGridAdapter.ServerViewHolder>(DiffCallback) {
    class ServerViewHolder(private var binding: ServerItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(server: Server) {
            binding.server = server
            binding.executePendingBindings()
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
            onClickListener.onClick(server)
        }
        holder.itemView.setOnLongClickListener {
            onLongClickListener.onLongClick(server)
        }
        holder.bind(server)
    }

    class OnClickListener(val clickListener: (server: Server) -> Unit) {
        fun onClick(server: Server) = clickListener(server)
    }

    class OnLongClickListener(val clickListener: (server: Server) -> Boolean) {
        fun onLongClick(server: Server) = clickListener(server)
    }
}
