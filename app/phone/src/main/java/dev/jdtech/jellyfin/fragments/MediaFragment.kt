package dev.jdtech.jellyfin.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.adapters.CollectionListAdapter
import dev.jdtech.jellyfin.databinding.FragmentMediaBinding
import dev.jdtech.jellyfin.dialogs.ErrorDialogFragment
import dev.jdtech.jellyfin.models.FindroidCollection
import dev.jdtech.jellyfin.utils.checkIfLoginRequired
import dev.jdtech.jellyfin.viewmodels.MediaViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import dev.jdtech.jellyfin.core.R as CoreR

@AndroidEntryPoint
class MediaFragment : Fragment() {

    private lateinit var binding: FragmentMediaBinding
    private val viewModel: MediaViewModel by viewModels()

    private var originalSoftInputMode: Int? = null

    private lateinit var errorDialog: ErrorDialogFragment

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentMediaBinding.inflate(inflater, container, false)

        binding.viewsRecyclerView.adapter =
            CollectionListAdapter(
                CollectionListAdapter.OnClickListener { library ->
                    navigateToLibraryFragment(library)
                },
            )

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    Timber.d("$uiState")
                    when (uiState) {
                        is MediaViewModel.UiState.Normal -> bindUiStateNormal(uiState)
                        is MediaViewModel.UiState.Loading -> bindUiStateLoading()
                        is MediaViewModel.UiState.Error -> bindUiStateError(uiState)
                    }
                }
            }
        }

        binding.errorLayout.errorRetryButton.setOnClickListener {
            viewModel.loadData()
        }

        binding.errorLayout.errorDetailsButton.setOnClickListener {
            errorDialog.show(parentFragmentManager, ErrorDialogFragment.TAG)
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(CoreR.menu.media_menu, menu)

                    val search = menu.findItem(CoreR.id.action_search)
                    val searchView = search.actionView as SearchView
                    searchView.queryHint = getString(CoreR.string.search_hint)

                    searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
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

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return true
                }
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED,
        )
    }

    override fun onStart() {
        super.onStart()
        requireActivity().window.let {
            originalSoftInputMode = it.attributes?.softInputMode
            it.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        }
    }

    override fun onStop() {
        super.onStop()
        originalSoftInputMode?.let { activity?.window?.setSoftInputMode(it) }
    }

    private fun bindUiStateNormal(uiState: MediaViewModel.UiState.Normal) {
        binding.loadingIndicator.isVisible = false
        binding.viewsRecyclerView.isVisible = true
        binding.errorLayout.errorPanel.isVisible = false
        val adapter = binding.viewsRecyclerView.adapter as CollectionListAdapter
        adapter.submitList(uiState.collections)
    }

    private fun bindUiStateLoading() {
        binding.loadingIndicator.isVisible = true
        binding.errorLayout.errorPanel.isVisible = false
    }

    private fun bindUiStateError(uiState: MediaViewModel.UiState.Error) {
        errorDialog = ErrorDialogFragment.newInstance(uiState.error)
        binding.loadingIndicator.isVisible = false
        binding.viewsRecyclerView.isVisible = false
        binding.errorLayout.errorPanel.isVisible = true
        checkIfLoginRequired(uiState.error.message)
    }

    private fun navigateToLibraryFragment(library: FindroidCollection) {
        findNavController().navigate(
            MediaFragmentDirections.actionNavigationMediaToLibraryFragment(
                libraryId = library.id,
                libraryName = library.name,
                libraryType = library.type,
            ),
        )
    }

    private fun navigateToSearchResultFragment(query: String) {
        findNavController().navigate(
            MediaFragmentDirections.actionNavigationMediaToSearchResultFragment(query),
        )
    }
}
