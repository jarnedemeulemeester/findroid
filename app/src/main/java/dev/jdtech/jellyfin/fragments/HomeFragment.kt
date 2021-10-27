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
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.adapters.HomeEpisodeListAdapter
import dev.jdtech.jellyfin.adapters.ViewItemListAdapter
import dev.jdtech.jellyfin.adapters.ViewListAdapter
import dev.jdtech.jellyfin.databinding.FragmentHomeBinding
import dev.jdtech.jellyfin.dialogs.ErrorDialogFragment
import dev.jdtech.jellyfin.models.ContentType
import dev.jdtech.jellyfin.models.ContentType.EPISODE
import dev.jdtech.jellyfin.models.ContentType.MOVIE
import dev.jdtech.jellyfin.models.ContentType.TVSHOW
import dev.jdtech.jellyfin.utils.checkIfLoginRequired
import dev.jdtech.jellyfin.utils.contentType
import dev.jdtech.jellyfin.utils.toggleVisibility
import dev.jdtech.jellyfin.viewmodels.HomeViewModel
import dev.jdtech.jellyfin.viewmodels.HomeViewModel.Loading
import dev.jdtech.jellyfin.viewmodels.HomeViewModel.LoadingError
import org.jellyfin.sdk.model.api.BaseItemDto

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private val viewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        findNavController().graph.startDestination = R.id.homeFragment
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.home_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                navigateToSettingsFragment()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)

        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel

        setupView()
        bindState()

        return binding.root
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
                when (item.contentType()) {
                    EPISODE -> navigateToEpisodeBottomSheetFragment(item)
                    MOVIE -> navigateToMediaInfoFragment(item)
                    else -> Toast.makeText(requireContext(), R.string.unknown_error, LENGTH_LONG)
                        .show()
                }
            })
    }

    private fun bindState() {
        viewModel.onStateUpdate(lifecycleScope) { state ->
            when (state) {
                is Loading -> bindLoading(state)
                is LoadingError -> bindError(state)
            }
        }
    }

    private fun bindError(state: LoadingError) {
        checkIfLoginRequired(state.message)
        binding.errorLayout.errorPanel.toggleVisibility()
        binding.viewsRecyclerView.toggleVisibility()

        binding.errorLayout.errorDetailsButton.setOnClickListener {
            ErrorDialogFragment(state.message).show(
                parentFragmentManager,
                "errordialog"
            )
        }

        binding.errorLayout.errorRetryButton.setOnClickListener {
            viewModel.refreshData()
        }
    }

    private fun bindLoading(state: Loading) {
        binding.errorLayout.errorPanel.visibility = View.GONE
        binding.viewsRecyclerView.visibility = View.VISIBLE

        binding.loadingIndicator.visibility = when {
            state.inProgress && binding.refreshLayout.isRefreshing -> View.GONE
            state.inProgress -> View.VISIBLE
            else -> {
                binding.refreshLayout.isRefreshing = false
                View.GONE
            }
        }
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
        if (item.contentType() == EPISODE) {
            findNavController().navigate(
                HomeFragmentDirections.actionNavigationHomeToMediaInfoFragment(
                    item.seriesId!!,
                    item.seriesName,
                    TVSHOW.type
                )
            )
        } else {
            findNavController().navigate(
                HomeFragmentDirections.actionNavigationHomeToMediaInfoFragment(
                    item.id,
                    item.name,
                    item.type ?: ContentType.UNKNOWN.type
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