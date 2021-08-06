package dev.jdtech.jellyfin.fragments

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.databinding.EpisodeBottomSheetBinding
import dev.jdtech.jellyfin.models.PlayerItem
import dev.jdtech.jellyfin.viewmodels.EpisodeBottomSheetViewModel

@AndroidEntryPoint
class EpisodeBottomSheetFragment : BottomSheetDialogFragment() {
    private val args: EpisodeBottomSheetFragmentArgs by navArgs()

    private lateinit var binding: EpisodeBottomSheetBinding
    private val viewModel: EpisodeBottomSheetViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = EpisodeBottomSheetBinding.inflate(inflater, container, false)

        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel

        binding.playButton.setOnClickListener {
            binding.playButton.setImageResource(android.R.color.transparent)
            binding.progressCircular.visibility = View.VISIBLE
            viewModel.preparePlayer()
        }

        binding.checkButton.setOnClickListener {
            when (viewModel.played.value) {
                true -> viewModel.markAsUnplayed(args.episodeId)
                false -> viewModel.markAsPlayed(args.episodeId)
            }
        }

        binding.favoriteButton.setOnClickListener {
            when (viewModel.favorite.value) {
                true -> viewModel.unmarkAsFavorite(args.episodeId)
                false -> viewModel.markAsFavorite(args.episodeId)
            }
        }

        viewModel.item.observe(viewLifecycleOwner, { episode ->
            if (episode.userData?.playedPercentage != null) {
                binding.progressBar.layoutParams.width = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    (episode.userData?.playedPercentage?.times(1.26))!!.toFloat(),
                    context?.resources?.displayMetrics
                ).toInt()
                binding.progressBar.visibility = View.VISIBLE
            }
            binding.communityRating.visibility = when (episode.communityRating != null) {
                false -> View.GONE
                true -> View.VISIBLE
            }
        })

        viewModel.played.observe(viewLifecycleOwner, {
            val drawable = when (it) {
                true -> R.drawable.ic_check_filled
                false -> R.drawable.ic_check
            }

            binding.checkButton.setImageResource(drawable)
        })

        viewModel.favorite.observe(viewLifecycleOwner, {
            val drawable = when (it) {
                true -> R.drawable.ic_heart_filled
                false -> R.drawable.ic_heart
            }

            binding.favoriteButton.setImageResource(drawable)
        })

        viewModel.navigateToPlayer.observe(viewLifecycleOwner, {
            if (it) {
                navigateToPlayerActivity(
                    viewModel.playerItems.toTypedArray(),
                    viewModel.item.value!!.userData!!.playbackPositionTicks.div(10000)
                )
                viewModel.doneNavigateToPlayer()
                binding.playButton.setImageDrawable(ContextCompat.getDrawable(requireActivity(), R.drawable.ic_play))
                binding.progressCircular.visibility = View.INVISIBLE
            }
        })

        viewModel.loadEpisode(args.episodeId)

        return binding.root
    }

    private fun navigateToPlayerActivity(
        playerItems: Array<PlayerItem>,
        playbackPosition: Long
    ) {
        findNavController().navigate(
            EpisodeBottomSheetFragmentDirections.actionEpisodeBottomSheetFragmentToPlayerActivity(
                playerItems,
                playbackPosition
            )
        )
    }
}