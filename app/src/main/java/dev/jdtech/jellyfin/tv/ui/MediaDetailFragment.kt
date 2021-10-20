package dev.jdtech.jellyfin.tv.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.databinding.MediaDetailFragmentBinding
import dev.jdtech.jellyfin.dialogs.VideoVersionDialogFragment
import dev.jdtech.jellyfin.models.PlayerItem
import dev.jdtech.jellyfin.viewmodels.MediaInfoViewModel

@AndroidEntryPoint
internal class MediaDetailFragment : Fragment() {

    private lateinit var binding: MediaDetailFragmentBinding

    private val viewModel: MediaInfoViewModel by viewModels()
    private val detailViewModel: MediaDetailViewModel by viewModels()

    private val args: MediaDetailFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.loadData(args.itemId, args.itemType)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = MediaDetailFragmentBinding.inflate(inflater)
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.item = detailViewModel.transformData(viewModel.item, resources) {
            bindActions(it)
            bindState(it)
        }
    }

    private fun bindState(state: MediaDetailViewModel.State) {
        viewModel.navigateToPlayer.observe(viewLifecycleOwner) { playerItems ->
            if (playerItems != null) {
                navigateToPlayerActivity(
                    playerItems
                )
                viewModel.doneNavigatingToPlayer()
                binding.playButton.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireActivity(),
                        R.drawable.ic_play
                    )
                )
                binding.progressCircular.isVisible = false
            }
        }

        if (state.episodeTitle != null) {
            with(binding.episodeTitle) {
                text = state.episodeTitle
                isVisible = true
            }
        }
    }

    private fun bindActions(state: MediaDetailViewModel.State) {
        binding.playButton.setOnClickListener {
            binding.progressCircular.isVisible = true
            viewModel.attemptToPlayMedia(args.itemType) {
                VideoVersionDialogFragment(viewModel).show(
                    parentFragmentManager,
                    "videoversiondialog"
                )
            }
        }

        if (state.trailerUrl != null) {
            with(binding.trailerButton) {
                isVisible = true
                setOnClickListener { playTrailer(state.trailerUrl) }
            }
        } else {
            binding.trailerButton.isVisible = false
        }

        if (state.isPlayed) {
            with(binding.checkButton) {
                setImageDrawable(resources.getDrawable(R.drawable.ic_check_filled))
                setOnClickListener { viewModel.markAsUnplayed(args.itemId) }
            }
        } else {
            with(binding.checkButton) {
                setImageDrawable(resources.getDrawable(R.drawable.ic_check))
                setOnClickListener { viewModel.markAsPlayed(args.itemId) }
            }
        }

        if (state.isFavorite) {
            with(binding.favoriteButton) {
                setImageDrawable(resources.getDrawable(R.drawable.ic_heart_filled))
                setOnClickListener { viewModel.unmarkAsFavorite(args.itemId) }
            }
        } else {
            with(binding.favoriteButton) {
                setImageDrawable(resources.getDrawable(R.drawable.ic_heart))
                setOnClickListener { viewModel.markAsFavorite(args.itemId) }
            }
        }
    }

    private fun playTrailer(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    private fun navigateToPlayerActivity(
        playerItems: Array<PlayerItem>,
    ) {
        findNavController().navigate(
            MediaDetailFragmentDirections.actionMediaDetailFragmentToPlayerActivity(
                playerItems
            )
        )
    }
}