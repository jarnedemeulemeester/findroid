package dev.jdtech.jellyfin.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.adapters.EpisodeListAdapter
import dev.jdtech.jellyfin.databinding.FragmentSeasonBinding
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
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.viewModel = viewModel

        val snackbar =
            Snackbar.make(
                binding.mainLayout,
                getString(R.string.error_loading_data),
                Snackbar.LENGTH_INDEFINITE
            )
        snackbar.setAction(getString(R.string.retry)) {
            viewModel.loadEpisodes(args.seriesId, args.seasonId)
        }

        viewModel.error.observe(viewLifecycleOwner, { error ->
            if (error) {
                snackbar.show()
            }
        })

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