package dev.jdtech.jellyfin.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.adapters.CollectionListAdapter
import dev.jdtech.jellyfin.databinding.FragmentMediaBinding
import dev.jdtech.jellyfin.viewmodels.MediaViewModel
import org.jellyfin.sdk.model.api.BaseItemDto

@AndroidEntryPoint
class MediaFragment : Fragment() {

    private lateinit var binding: FragmentMediaBinding
    private val viewModel: MediaViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMediaBinding.inflate(inflater, container, false)

        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.viewsRecyclerView.adapter =
            CollectionListAdapter(CollectionListAdapter.OnClickListener { library ->
                nagivateToLibraryFragment(library)
            })

        viewModel.finishedLoading.observe(viewLifecycleOwner, {
            if (it) {
                binding.loadingIndicator.visibility = View.GONE
            }
        })

        return binding.root
    }

    private fun nagivateToLibraryFragment(library: BaseItemDto) {
        findNavController().navigate(
            MediaFragmentDirections.actionNavigationMediaToLibraryFragment(
                library.id,
                library.name
            )
        )
    }
}