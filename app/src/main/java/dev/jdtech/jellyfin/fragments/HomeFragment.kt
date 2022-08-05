package dev.jdtech.jellyfin.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
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
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.adapters.HomeEpisodeListAdapter
import dev.jdtech.jellyfin.adapters.ViewItemListAdapter
import dev.jdtech.jellyfin.adapters.ViewListAdapter
import dev.jdtech.jellyfin.databinding.FragmentHomeBinding
import dev.jdtech.jellyfin.dialogs.ErrorDialogFragment
import dev.jdtech.jellyfin.utils.checkIfLoginRequired
import dev.jdtech.jellyfin.viewmodels.HomeViewModel
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import timber.log.Timber

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private val viewModel: HomeViewModel by viewModels()

    private lateinit var errorDialog: ErrorDialogFragment

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)

        setupView()
        bindState()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.home_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_settings -> {
                        navigateToSettingsFragment()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        viewModel.loadData(updateCapabilities = true)
    }

    override fun onResume() {
        super.onResume()

        viewModel.refreshData()
    }

    private fun setupView() {
        binding.refreshLayout.setOnRefreshListener {
            viewModel.refreshData()
        }

        binding.viewsRecyclerView.adapter = ViewListAdapter(
            onClickListener = ViewListAdapter.OnClickListener { navigateToLibraryFragment(it) },
            onItemClickListener = ViewItemListAdapter.OnClickListener {
                navigateToMediaInfoFragment(it)
            },
            onNextUpClickListener = HomeEpisodeListAdapter.OnClickListener { item ->
                when (item.type) {
                    BaseItemKind.EPISODE -> navigateToEpisodeBottomSheetFragment(item)
                    BaseItemKind.MOVIE -> navigateToMediaInfoFragment(item)
                    else -> Toast.makeText(requireContext(), R.string.unknown_error, LENGTH_LONG)
                        .show()
                }
            })

        binding.errorLayout.errorRetryButton.setOnClickListener {
            viewModel.refreshData()
        }

        binding.errorLayout.errorDetailsButton.setOnClickListener {
            errorDialog.show(parentFragmentManager, "errordialog")
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
        errorDialog = ErrorDialogFragment(uiState.error)
        binding.loadingIndicator.isVisible = false
        binding.refreshLayout.isRefreshing = false
        binding.viewsRecyclerView.isVisible = false
        binding.errorLayout.errorPanel.isVisible = true
        checkIfLoginRequired(uiState.error.message)
    }

    private fun navigateToLibraryFragment(view: dev.jdtech.jellyfin.models.View) {
        findNavController().navigate(
            HomeFragmentDirections.actionNavigationHomeToLibraryFragment(
                view.id,
                view.name,
                view.type
            )
        )
    }

    private fun navigateToMediaInfoFragment(item: BaseItemDto) {
        if (item.type == BaseItemKind.EPISODE) {
            findNavController().navigate(
                HomeFragmentDirections.actionNavigationHomeToMediaInfoFragment(
                    item.seriesId!!,
                    item.seriesName,
                    BaseItemKind.SERIES
                )
            )
        } else {
            findNavController().navigate(
                HomeFragmentDirections.actionNavigationHomeToMediaInfoFragment(
                    item.id,
                    item.name,
                    item.type
                )
            )
        }
    }

    private fun navigateToEpisodeBottomSheetFragment(episode: BaseItemDto) {
        findNavController().navigate(
            HomeFragmentDirections.actionNavigationHomeToEpisodeBottomSheetFragment(
                episode.id
            )
        )
    }

    private fun navigateToSettingsFragment() {
        findNavController().navigate(
            HomeFragmentDirections.actionNavigationHomeToNavigationSettings()
        )
    }
}