package dev.jdtech.jellyfin.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.adapters.DownloadEpisodeListAdapter
import dev.jdtech.jellyfin.adapters.DownloadHomeEpisodeListAdapter
import dev.jdtech.jellyfin.adapters.EpisodeListAdapter
import dev.jdtech.jellyfin.databinding.FragmentDownloadSeriesBinding
import dev.jdtech.jellyfin.dialogs.ErrorDialogFragment
import dev.jdtech.jellyfin.models.PlayerItem
import dev.jdtech.jellyfin.utils.checkIfLoginRequired
import dev.jdtech.jellyfin.viewmodels.DownloadSeriesViewModel
import org.jellyfin.sdk.model.api.BaseItemDto
import java.util.*

@AndroidEntryPoint
class DownloadSeriesFragment : Fragment() {

    private lateinit var binding: FragmentDownloadSeriesBinding
    private val viewModel: DownloadSeriesViewModel by viewModels()

    private val args: DownloadSeriesFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDownloadSeriesBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.viewModel = viewModel

        viewModel.error.observe(viewLifecycleOwner, { error ->
            if (error != null) {
                checkIfLoginRequired(error)
                binding.errorLayout.errorPanel.visibility = View.VISIBLE
                binding.episodesRecyclerView.visibility = View.GONE
            } else {
                binding.errorLayout.errorPanel.visibility = View.GONE
                binding.episodesRecyclerView.visibility = View.VISIBLE
            }
        })

        binding.errorLayout.errorRetryButton.setOnClickListener {
            viewModel.loadEpisodes(args.seriesMetadata)
        }

        binding.errorLayout.errorDetailsButton.setOnClickListener {
            ErrorDialogFragment(viewModel.error.value ?: getString(R.string.unknown_error)).show(parentFragmentManager, "errordialog")
        }

        binding.episodesRecyclerView.adapter =
            DownloadEpisodeListAdapter(DownloadEpisodeListAdapter.OnClickListener { episode ->
                navigateToEpisodeBottomSheetFragment(episode)
            }, args.seriesMetadata)

        viewModel.loadEpisodes(args.seriesMetadata)
    }

    private fun navigateToEpisodeBottomSheetFragment(episode: PlayerItem) {
        findNavController().navigate(
            DownloadSeriesFragmentDirections.actionDownloadSeriesFragmentToEpisodeBottomSheetFragment(
                UUID.randomUUID(),
                episode,
                isOffline = true
            )
        )
    }
}