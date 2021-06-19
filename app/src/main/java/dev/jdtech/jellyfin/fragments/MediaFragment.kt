package dev.jdtech.jellyfin.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import dev.jdtech.jellyfin.adapters.CollectionListAdapter
import dev.jdtech.jellyfin.databinding.FragmentMediaBinding
import dev.jdtech.jellyfin.viewmodels.MediaViewModel

class MediaFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentMediaBinding.inflate(inflater, container, false)
        val viewModel = ViewModelProvider(this).get(MediaViewModel::class.java)

        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.viewsRecyclerView.adapter =
            CollectionListAdapter(CollectionListAdapter.OnClickListener { library ->
                findNavController().navigate(
                    MediaFragmentDirections.actionNavigationMediaToLibraryFragment(
                        library.id
                    )
                )

            })

        viewModel.finishedLoading.observe(viewLifecycleOwner, {
            if (it) {
                binding.loadingIncicator.visibility = View.GONE
            }
        })

        return binding.root
    }
}