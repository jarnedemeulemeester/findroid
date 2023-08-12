package dev.jdtech.jellyfin.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.jdtech.jellyfin.bindUserImage
import dev.jdtech.jellyfin.databinding.UserItemBinding
import dev.jdtech.jellyfin.models.User

class UserLoginListAdapter(
    private val clickListener: (user: User) -> Unit,
) : ListAdapter<User, UserLoginListAdapter.UserLoginViewHolder>(DiffCallback) {
    class UserLoginViewHolder(private var binding: UserItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(user: User) {
            binding.userName.text = user.name
            bindUserImage(binding.userImage, user)
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): UserLoginViewHolder {
        return UserLoginViewHolder(
            UserItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            ),
        )
    }

    override fun onBindViewHolder(holder: UserLoginViewHolder, position: Int) {
        val user = getItem(position)
        holder.itemView.setOnClickListener { clickListener(user) }
        holder.bind(user)
    }
}
