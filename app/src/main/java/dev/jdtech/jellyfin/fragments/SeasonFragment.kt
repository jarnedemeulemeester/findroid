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
import dev.jdtech.jellyfin.adapters.EpisodeListAdapter
import dev.jdtech.jellyfin.databinding.FragmentSeasonBinding
import dev.jdtech.jellyfin.dialogs.ErrorDialogFragment
import dev.jdtech.jellyfin.utils.checkIfLoginRequired
import dev.jdtech.jellyfin.viewmodels.SeasonViewModel
import org.jellyfin.sdk.model.api.BaseItemDto

@AndroidEntryPoint
class SeasonFragment : Fragment() {

    private lateinit var binding: FragmentSeasonBinding
    private val viewModel: SeasonViewModel by viewModels()

    private val args: SeasonFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSeasonBinding.inflate(inflater, container, false)
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
            viewModel.loadEpisodes(args.seriesId, args.seasonId)
        }

        binding.errorLayout.errorDetailsButton.setOnClickListener {
            ErrorDialogFragment(viewModel.error.value ?: getString(R.string.unknown_error)).show(parentFragmentManager, "errordialog")
        }

        viewModel.finishedLoading.observe(viewLifecycleOwner, {
            binding.loadingIndicator.visibility = if (it) View.GONE else View.VISIBLE
        })

        binding.episodesRecyclerView.adapter =
            EpisodeListAdapter(EpisodeListAdapter.OnClickListener { episode ->
                navigateToEpisodeBottomSheetFragment(episode)
            }, args.seriesId, args.seriesName, args.seasonId, args.seasonName)

        viewModel.loadEpisodes(args.seriesId, args.seasonId)
    }

    private fun navigateToEpisodeBottomSheetFragment(episode: BaseItemDto) {
        findNavController().navigate(
            SeasonFragmentDirections.actionSeasonFragmentToEpisodeBottomSheetFragment(
                episode.id
            )
        )
    }
}