package dev.jdtech.jellyfin.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.adapters.EpisodeListAdapter
import dev.jdtech.jellyfin.databinding.FragmentSeasonBinding
import dev.jdtech.jellyfin.dialogs.ErrorDialogFragment
import dev.jdtech.jellyfin.dialogs.getStorageSelectionDialog
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.UiText
import dev.jdtech.jellyfin.utils.checkIfLoginRequired
import dev.jdtech.jellyfin.viewmodels.SeasonEvent
import dev.jdtech.jellyfin.viewmodels.SeasonViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import dev.jdtech.jellyfin.core.R as CoreR

@AndroidEntryPoint
class SeasonFragment : Fragment() {

    private lateinit var binding: FragmentSeasonBinding
    private val viewModel: SeasonViewModel by viewModels()
    private val args: SeasonFragmentArgs by navArgs()

    private lateinit var errorDialog: ErrorDialogFragment
    private lateinit var downloadPreparingDialog: AlertDialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentSeasonBinding.inflate(inflater, container, false)
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(dev.jdtech.jellyfin.core.R.menu.season_menu, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return when (menuItem.itemId) {
                        CoreR.id.action_download_season -> {
                            if (requireContext().getExternalFilesDirs(null).filterNotNull().size > 1) {
                                val storageDialog = getStorageSelectionDialog(
                                    requireContext(),
                                    onItemSelected = { storageIndex ->
                                        createEpisodesToDownloadDialog(storageIndex)
                                    },
                                    onCancel = {
                                    },
                                )
                                return true
                            }
                            createEpisodesToDownloadDialog()
                            return true
                        }
                        else -> false
                    }
                }
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED,
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
                    viewModel.downloadStatus.collect { (status, progress) ->
                        when (status) {
                            10 -> {
                                downloadPreparingDialog.dismiss()
                            }
                        }
                    }
                }

                launch {
                    viewModel.downloadError.collect { uiText ->
                        createErrorDialog(uiText)
                    }
                }

                launch {
                    viewModel.navigateBack.collect {
                        if (it) findNavController().navigateUp()
                        viewModel.eventsChannelFlow.collect { event ->
                            when (event) {
                                is SeasonEvent.NavigateBack -> findNavController().navigateUp()
                            }
                        }
                    }
                }
            }
        }

        binding.errorLayout.errorRetryButton.setOnClickListener {
            viewModel.loadEpisodes(args.seriesId, args.seasonId, args.offline)
        }

        binding.errorLayout.errorDetailsButton.setOnClickListener {
            errorDialog.show(parentFragmentManager, ErrorDialogFragment.TAG)
        }

        binding.episodesRecyclerView.adapter =
            EpisodeListAdapter { episode ->
                navigateToEpisodeBottomSheetFragment(episode)
            }
    }

    override fun onResume() {
        super.onResume()

        viewModel.loadEpisodes(args.seriesId, args.seasonId, args.offline)
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

    private fun createDownloadPreparingDialog() {
        val builder = MaterialAlertDialogBuilder(requireContext())
        downloadPreparingDialog = builder
            .setTitle(CoreR.string.preparing_download)
            .setView(R.layout.preparing_download_dialog)
            .setCancelable(false)
            .create()
        downloadPreparingDialog.show()
    }

    private fun createErrorDialog(uiText: UiText) {
        val builder = MaterialAlertDialogBuilder(requireContext())
        builder
            .setTitle(CoreR.string.downloading_error)
            .setMessage(uiText.asString(requireContext().resources))
            .setPositiveButton(getString(dev.jdtech.jellyfin.core.R.string.close)) { _, _ ->
            }
        builder.show()
    }

    private fun createEpisodesToDownloadDialog(storageIndex: Int = 0) {
        val builder = MaterialAlertDialogBuilder(requireContext())
        val dialog = builder
            .setTitle(CoreR.string.download_season_dialog_title)
            .setMessage(CoreR.string.download_season_dialog_question)
            .setPositiveButton(CoreR.string.download_season_dialog_download_all) { _, _ ->
                createDownloadPreparingDialog()
                viewModel.download(storageIndex = storageIndex, downloadWatched = true)
            }
            .setNegativeButton(CoreR.string.download_season_dialog_download_unwatched) { _, _ ->
                createDownloadPreparingDialog()
                viewModel.download(storageIndex = storageIndex, downloadWatched = false)
            }
            .create()
        dialog.show()
    }

    private fun navigateToEpisodeBottomSheetFragment(episode: FindroidEpisode) {
        findNavController().navigate(
            SeasonFragmentDirections.actionSeasonFragmentToEpisodeBottomSheetFragment(
                episode.id,
            ),
        )
    }
}
