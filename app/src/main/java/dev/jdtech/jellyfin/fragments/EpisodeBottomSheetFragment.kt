package dev.jdtech.jellyfin.fragments

import android.net.Uri
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
import dev.jdtech.jellyfin.dialogs.ErrorDialogFragment
import dev.jdtech.jellyfin.models.PlayerItem
import dev.jdtech.jellyfin.utils.requestDownload
import dev.jdtech.jellyfin.viewmodels.EpisodeBottomSheetViewModel
import java.util.*

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
            viewModel.preparePlayerItems()
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
                )
                viewModel.doneNavigateToPlayer()
                binding.playButton.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireActivity(),
                        R.drawable.ic_play
                    )
                )
                binding.progressCircular.visibility = View.INVISIBLE
            }
        })

        viewModel.downloadEpisode.observe(viewLifecycleOwner, {
            if (it) {
                requestDownload(Uri.parse(viewModel.downloadRequestItem.uri), viewModel.downloadRequestItem, this)
                viewModel.doneDownloadEpisode()
            }
        })

        viewModel.playerItemsError.observe(viewLifecycleOwner, { errorMessage ->
            if (errorMessage != null) {
                binding.playerItemsError.visibility = View.VISIBLE
                binding.playButton.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireActivity(),
                        R.drawable.ic_play
                    )
                )
                binding.progressCircular.visibility = View.INVISIBLE
            } else {
                binding.playerItemsError.visibility = View.GONE
            }
        })

        binding.playerItemsErrorDetails.setOnClickListener {
            ErrorDialogFragment(
                viewModel.playerItemsError.value ?: getString(R.string.unknown_error)
            ).show(parentFragmentManager, "errordialog")
        }

        if(args.episodeId != null){
            val episodeId: UUID = args.episodeId!!
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