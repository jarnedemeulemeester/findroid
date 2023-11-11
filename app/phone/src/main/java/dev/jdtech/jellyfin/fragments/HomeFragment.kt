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
import dev.jdtech.jellyfin.AppPreferences
import dev.jdtech.jellyfin.adapters.ViewListAdapter
import dev.jdtech.jellyfin.databinding.FragmentHomeBinding
import dev.jdtech.jellyfin.dialogs.ErrorDialogFragment
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.FindroidMovie
import dev.jdtech.jellyfin.models.FindroidShow
import dev.jdtech.jellyfin.utils.checkIfLoginRequired
import dev.jdtech.jellyfin.utils.restart
import dev.jdtech.jellyfin.viewmodels.HomeViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import dev.jdtech.jellyfin.core.R as CoreR

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private val viewModel: HomeViewModel by viewModels()

    private var originalSoftInputMode: Int? = null

    private lateinit var errorDialog: ErrorDialogFragment

    @Inject
    lateinit var appPreferences: AppPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)

        setupView()
        bindState()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(CoreR.menu.home_menu, menu)
                    val settings = menu.findItem(CoreR.id.action_settings)
                    val search = menu.findItem(CoreR.id.action_search)
                    val searchView = search.actionView as SearchView
                    searchView.queryHint = getString(CoreR.string.search_hint)

                    search.setOnActionExpandListener(
                        object : MenuItem.OnActionExpandListener {
                            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                                settings.isVisible = false
                                return true
                            }

                            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                                settings.isVisible = true
                                return true
                            }
                        },
                    )

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
                    return when (menuItem.itemId) {
                        CoreR.id.action_settings -> {
                            navigateToSettingsFragment()
                            true
                        }
                        else -> false
                    }
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

    override fun onResume() {
        super.onResume()

        viewModel.loadData()
    }

    override fun onStop() {
        super.onStop()

        originalSoftInputMode?.let { activity?.window?.setSoftInputMode(it) }
    }

    private fun setupView() {
        binding.refreshLayout.setOnRefreshListener {
            viewModel.loadData()
        }

        binding.viewsRecyclerView.adapter = ViewListAdapter(
            onClickListener = { navigateToLibraryFragment(it) },
            onItemClickListener = {
                navigateToMediaItem(it)
            },
            onOnlineClickListener = {
                appPreferences.offlineMode = false
                activity?.restart()
            },
        )

        binding.errorLayout.errorRetryButton.setOnClickListener {
            viewModel.loadData()
        }

        binding.errorLayout.errorDetailsButton.setOnClickListener {
            errorDialog.show(parentFragmentManager, ErrorDialogFragment.TAG)
        }
    }

    private fun bindState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    Timber.d("$uiState")
                    when (uiState) {
                        is HomeViewModel.UiState.Normal -> bindUiStateNormal(uiState)
                        is HomeViewModel.UiState.Loading -> bindUiStateLoading()
                        is HomeViewModel.UiState.Error -> bindUiStateError(uiState)
                    }
                }
            }
        }
    }

    private fun bindUiStateNormal(uiState: HomeViewModel.UiState.Normal) {
        uiState.apply {
            val adapter = binding.viewsRecyclerView.adapter as ViewListAdapter
            adapter.submitList(uiState.homeItems)
        }
        binding.loadingIndicator.isVisible = false
        binding.refreshLayout.isRefreshing = false
        binding.viewsRecyclerView.isVisible = true
    }

    private fun bindUiStateLoading() {
        binding.loadingIndicator.isVisible = true
        binding.errorLayout.errorPanel.isVisible = false
    }

    private fun bindUiStateError(uiState: HomeViewModel.UiState.Error) {
        errorDialog = ErrorDialogFragment.newInstance(uiState.error)
        binding.loadingIndicator.isVisible = false
        binding.refreshLayout.isRefreshing = false
        binding.viewsRecyclerView.isVisible = false
        binding.errorLayout.errorPanel.isVisible = true
        checkIfLoginRequired(uiState.error.message)
    }

    private fun navigateToLibraryFragment(view: dev.jdtech.jellyfin.models.View) {
        findNavController().navigate(
            HomeFragmentDirections.actionNavigationHomeToLibraryFragment(
                libraryId = view.id,
                libraryName = view.name,
                libraryType = view.type,
            ),
        )
    }

    private fun navigateToMediaItem(item: FindroidItem) {
        when (item) {
            is FindroidMovie -> {
                findNavController().navigate(
                    HomeFragmentDirections.actionNavigationHomeToMovieFragment(
                        item.id,
                        item.name,
                    ),
                )
            }
            is FindroidShow -> {
                findNavController().navigate(
                    HomeFragmentDirections.actionNavigationHomeToShowFragment(
                        item.id,
                        item.name,
                    ),
                )
            }
            is FindroidEpisode -> {
                findNavController().navigate(
                    HomeFragmentDirections.actionNavigationHomeToEpisodeBottomSheetFragment(
                        item.id,
                    ),
                )
            }
        }
    }

    private fun navigateToSettingsFragment() {
        findNavController().navigate(
            HomeFragmentDirections.actionHomeFragmentToSettingsFragment(),
        )
    }

    private fun navigateToSearchResultFragment(query: String) {
        findNavController().navigate(
            HomeFragmentDirections.actionHomeFragmentToSearchResultFragment(query),
        )
    }
}
