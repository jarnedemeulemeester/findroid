package dev.jdtech.jellyfin.fragments

import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.adapters.CollectionListAdapter
import dev.jdtech.jellyfin.databinding.FragmentMediaBinding
import dev.jdtech.jellyfin.dialogs.ErrorDialogFragment
import dev.jdtech.jellyfin.utils.checkIfLoginRequired
import dev.jdtech.jellyfin.viewmodels.MediaViewModel
import org.jellyfin.sdk.model.api.BaseItemDto

@AndroidEntryPoint
class MediaFragment : Fragment() {

    private lateinit var binding: FragmentMediaBinding
    private val viewModel: MediaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.media_menu, menu)

        val search = menu.findItem(R.id.action_search)
        val searchView = search.actionView as SearchView
        searchView.queryHint = getString(R.string.search_hint)

        searchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(p0: String?): Boolean {
                if (p0 != null) {
                    navigateToSearchResultFragment(p0)
                }
                return true
            }

            override fun onQueryTextChange(p0: String?): Boolean {
                return false
            }
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMediaBinding.inflate(inflater, container, false)

        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        binding.viewsRecyclerView.adapter =
            CollectionListAdapter(CollectionListAdapter.OnClickListener { library ->
                navigateToLibraryFragment(library)
            })

        viewModel.finishedLoading.observe(viewLifecycleOwner, {
            binding.loadingIndicator.visibility = if (it) View.GONE else View.VISIBLE
        })

        viewModel.error.observe(viewLifecycleOwner, { error ->
            if (error != null) {
                checkIfLoginRequired(error)
                binding.errorLayout.errorPanel.visibility = View.VISIBLE
                binding.viewsRecyclerView.visibility = View.GONE
            } else {
                binding.errorLayout.errorPanel.visibility = View.GONE
                binding.viewsRecyclerView.visibility = View.VISIBLE
            }
        })

        binding.errorLayout.errorRetryButton.setOnClickListener {
            viewModel.loadData()
        }

        binding.errorLayout.errorDetailsButton.setOnClickListener {
            ErrorDialogFragment(viewModel.error.value ?: getString(R.string.unknown_error)).show(parentFragmentManager, "errordialog")
        }

        return binding.root
    }

    private fun navigateToLibraryFragment(library: BaseItemDto) {
        findNavController().navigate(
            MediaFragmentDirections.actionNavigationMediaToLibraryFragment(
                library.id,
                library.name,
                library.collectionType,
            )
        )
    }

    private fun navigateToSearchResultFragment(query: String) {
        findNavController().navigate(
            MediaFragmentDirections.actionNavigationMediaToSearchResultFragment(query)
        )
    }
}