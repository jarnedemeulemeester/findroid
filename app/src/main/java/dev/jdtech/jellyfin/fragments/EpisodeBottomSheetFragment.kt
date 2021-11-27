package dev.jdtech.jellyfin.fragments

import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.databinding.EpisodeBottomSheetBinding
import dev.jdtech.jellyfin.dialogs.ErrorDialogFragment
import dev.jdtech.jellyfin.models.PlayerItem
import dev.jdtech.jellyfin.utils.requestDownload
import dev.jdtech.jellyfin.viewmodels.EpisodeBottomSheetViewModel
import dev.jdtech.jellyfin.viewmodels.PlayerViewModel
import timber.log.Timber
import java.util.*

@AndroidEntryPoint
class EpisodeBottomSheetFragment : BottomSheetDialogFragment() {
    private val args: EpisodeBottomSheetFragmentArgs by navArgs()

    private lateinit var binding: EpisodeBottomSheetBinding
    private val viewModel: EpisodeBottomSheetViewModel by viewModels()
    private val playerViewModel: PlayerViewModel by viewModels()

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
            viewModel.item.value?.let {
                if (!args.isOffline) {
                    playerViewModel.loadPlayerItems(it)
                } else {
                    playerViewModel.loadOfflinePlayerItems(viewModel.playerItems[0])
                }
            }
        }

        playerViewModel.onPlaybackRequested(lifecycleScope) { playerItems ->
            when (playerItems) {
                is PlayerViewModel.PlayerItemError -> bindPlayerItemsError(playerItems)
                is PlayerViewModel.PlayerItems -> bindPlayerItems(playerItems)
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

        viewModel.downloaded.observe(viewLifecycleOwner, {
            val drawable = when (it) {
                true -> R.drawable.ic_download_filled
                false -> R.drawable.ic_download
            }

            binding.downloadButton.setImageResource(drawable)
        })

        viewModel.downloadEpisode.observe(viewLifecycleOwner, {
            if (it) {
                requestDownload(Uri.parse(viewModel.downloadRequestItem.uri), viewModel.downloadRequestItem, this)
                viewModel.doneDownloadEpisode()
            }
        })

        if(!args.isOffline){
            val episodeId: UUID = args.episodeId
            binding.checkButton.setOnClickListener {
                when (viewModel.played.value) {
                    true -> viewModel.markAsUnplayed(episodeId)
                    false -> viewModel.markAsPlayed(episodeId)
                }
            }

            binding.favoriteButton.setOnClickListener {
                when (viewModel.favorite.value) {
                    true -> viewModel.unmarkAsFavorite(episodeId)
                    false -> viewModel.markAsFavorite(episodeId)
                }
            }

            binding.downloadButton.setOnClickListener {
                viewModel.loadDownloadRequestItem(episodeId)
            }

            binding.deleteButton.visibility = View.GONE

            viewModel.loadEpisode(episodeId)
        }else {
            val playerItem = args.playerItem!!
            viewModel.loadEpisode(playerItem)

            binding.deleteButton.setOnClickListener {
                viewModel.deleteEpisode()
                dismiss()
                findNavController().navigate(R.id.downloadFragment)
            }

            binding.checkButton.visibility = View.GONE
            binding.favoriteButton.visibility = View.GONE
            binding.downloadButton.visibility = View.GONE
        }

        return binding.root
    }

    private fun bindPlayerItems(items: PlayerViewModel.PlayerItems) {
        navigateToPlayerActivity(items.items.toTypedArray())
        binding.playButton.setImageDrawable(
            ContextCompat.getDrawable(
                requireActivity(),
                R.drawable.ic_play
            )
        )
        binding.progressCircular.visibility = View.INVISIBLE
    }

    private fun bindPlayerItemsError(error: PlayerViewModel.PlayerItemError) {
        Timber.e(error.message)

        binding.playerItemsError.isVisible = true
        binding.playButton.setImageDrawable(
            ContextCompat.getDrawable(
                requireActivity(),
                R.drawable.ic_play
            )
        )
        binding.progressCircular.visibility = View.INVISIBLE
        binding.playerItemsErrorDetails.setOnClickListener {
            ErrorDialogFragment(error.message).show(parentFragmentManager, "errordialog")
        }
    }

    private fun navigateToPlayerActivity(
        playerItems: Array<PlayerItem>,
    ) {
        findNavController().navigate(
            EpisodeBottomSheetFragmentDirections.actionEpisodeBottomSheetFragmentToPlayerActivity(
                playerItems,
            )
        )
    }
}