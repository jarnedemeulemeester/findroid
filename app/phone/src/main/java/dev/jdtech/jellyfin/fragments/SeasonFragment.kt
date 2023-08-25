package dev.jdtech.jellyfin.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
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
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.adapters.EpisodeListAdapter
import dev.jdtech.jellyfin.databinding.FragmentSeasonBinding
import dev.jdtech.jellyfin.dialogs.ErrorDialogFragment
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.PlayerItem
import dev.jdtech.jellyfin.utils.checkIfLoginRequired
import dev.jdtech.jellyfin.viewmodels.PlayerViewModel
import dev.jdtech.jellyfin.viewmodels.SeasonViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import dev.jdtech.jellyfin.core.R as CoreR

@AndroidEntryPoint
class SeasonFragment : Fragment() {

    private lateinit var binding: FragmentSeasonBinding
    private val viewModel: SeasonViewModel by viewModels()
    private val playerViewModel: PlayerViewModel by viewModels()
    private val args: SeasonFragmentArgs by navArgs()

    private lateinit var errorDialog: ErrorDialogFragment

    private var ascendingEpisodeOrder: Boolean = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentSeasonBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(CoreR.menu.season_menu, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return when (menuItem.itemId) {
                        CoreR.id.action_sort_order -> {
                            ascendingEpisodeOrder = !ascendingEpisodeOrder
                            menuItem.setIcon(
                                when (ascendingEpisodeOrder) {
                                    true -> CoreR.drawable.ic_arrow_down_0_1
                                    false -> CoreR.drawable.ic_arrow_down_1_0
                                }
                            )

                            viewModel.loadEpisodes(args.seriesId, args.seasonId, args.offline, ascendingEpisodeOrder)
                            true
                        }
                        else -> false
                    }
                }
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED,
        )

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { uiState ->
                        Timber.d("$uiState")
                        when (uiState) {
                            is SeasonViewModel.UiState.Normal -> bindUiStateNormal(uiState)
                            is SeasonViewModel.UiState.Loading -> bindUiStateLoading()
                            is SeasonViewModel.UiState.Error -> bindUiStateError(uiState)
                        }
                    }
                }

                launch {
                    viewModel.navigateBack.collect {
                        if (it) findNavController().navigateUp()
                    }
                }
            }
        }

        binding.errorLayout.errorRetryButton.setOnClickListener {
            viewModel.loadEpisodes(args.seriesId, args.seasonId, args.offline, ascendingEpisodeOrder)
        }

        playerViewModel.onPlaybackRequested(lifecycleScope) { playerItems ->
            when (playerItems) {
                is PlayerViewModel.PlayerItems -> {
                    navigateToPlayerActivity(playerItems.items.toTypedArray())
                }
                is PlayerViewModel.PlayerItemError -> {}
            }
        }

        binding.errorLayout.errorDetailsButton.setOnClickListener {
            errorDialog.show(parentFragmentManager, ErrorDialogFragment.TAG)
        }

        binding.episodesRecyclerView.adapter =
            EpisodeListAdapter(
                EpisodeListAdapter.OnClickListener { episode ->
                    navigateToEpisodeBottomSheetFragment(episode)
                },
            )
    }

    override fun onResume() {
        super.onResume()

        viewModel.loadEpisodes(args.seriesId, args.seasonId, args.offline, ascendingEpisodeOrder)
    }

    private fun bindUiStateNormal(uiState: SeasonViewModel.UiState.Normal) {
        uiState.apply {
            val adapter = binding.episodesRecyclerView.adapter as EpisodeListAdapter
            adapter.submitList(uiState.episodes)
        }
        binding.loadingIndicator.isVisible = false
        binding.episodesRecyclerView.isVisible = true
        binding.errorLayout.errorPanel.isVisible = false
    }

    private fun bindUiStateLoading() {
        binding.loadingIndicator.isVisible = true
        binding.errorLayout.errorPanel.isVisible = false
    }

    private fun bindUiStateError(uiState: SeasonViewModel.UiState.Error) {
        errorDialog = ErrorDialogFragment.newInstance(uiState.error)
        binding.loadingIndicator.isVisible = false
        binding.episodesRecyclerView.isVisible = false
        binding.errorLayout.errorPanel.isVisible = true
        checkIfLoginRequired(uiState.error.message)
    }

    private fun navigateToEpisodeBottomSheetFragment(episode: FindroidEpisode) {
        findNavController().navigate(
            SeasonFragmentDirections.actionSeasonFragmentToEpisodeBottomSheetFragment(
                episode.id,
            ),
        )
    }

    private fun navigateToPlayerActivity(
        playerItems: Array<PlayerItem>,
    ) {
        findNavController().navigate(
            SeasonFragmentDirections.actionSeasonFragmentToPlayerActivity(
                playerItems,
            ),
        )
    }
}
