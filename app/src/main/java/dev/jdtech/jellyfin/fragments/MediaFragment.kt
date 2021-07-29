package dev.jdtech.jellyfin.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.R
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

        val snackbar =
            Snackbar.make(
                binding.mainLayout,
                getString(R.string.error_loading_data),
                Snackbar.LENGTH_INDEFINITE
            )
        snackbar.setAction(getString(R.string.retry)) {
            viewModel.loadData()
        }

        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.viewsRecyclerView.adapter =
            CollectionListAdapter(CollectionListAdapter.OnClickListener { library ->
                navigateToLibraryFragment(library)
            })

        viewModel.finishedLoading.observe(viewLifecycleOwner, {
            binding.loadingIndicator.visibility = if (it) View.GONE else View.VISIBLE
        })

        viewModel.error.observe(viewLifecycleOwner, { error ->
            if (error) {
                snackbar.show()
            }
        })

        return binding.root
    }

    private fun navigateToLibraryFragment(library: BaseItemDto) {
        findNavController().navigate(
            MediaFragmentDirections.actionNavigationMediaToLibraryFragment(
                library.id,
                library.name
            )
        )
    }
}