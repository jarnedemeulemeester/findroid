package dev.jdtech.jellyfin.fragments

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.bindBaseItemImage
import dev.jdtech.jellyfin.databinding.EpisodeBottomSheetBinding
import dev.jdtech.jellyfin.dialogs.ErrorDialogFragment
import dev.jdtech.jellyfin.models.PlayerItem
import dev.jdtech.jellyfin.utils.setTintColor
import dev.jdtech.jellyfin.utils.setTintColorAttribute
import dev.jdtech.jellyfin.viewmodels.EpisodeBottomSheetViewModel
import dev.jdtech.jellyfin.viewmodels.PlayerViewModel
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.LocationType
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

        binding.playButton.setOnClickListener {
            binding.playButton.setImageResource(android.R.color.transparent)
            binding.progressCircular.isVisible = true
            viewModel.item?.let {
                if (!args.isOffline) {
                    playerViewModel.loadPlayerItems(it)
                } else {
                    playerViewModel.loadOfflinePlayerItems(viewModel.playerItems[0])
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.onUiState(viewLifecycleOwner.lifecycleScope) { uiState ->
                    Timber.d("$uiState")
                    when (uiState) {
                        is EpisodeBottomSheetViewModel.UiState.Normal -> bindUiStateNormal(uiState)
                        is EpisodeBottomSheetViewModel.UiState.Loading -> bindUiStateLoading()
                        is EpisodeBottomSheetViewModel.UiState.Error -> bindUiStateError(uiState)
                    }
                }
            }
        }

        playerViewModel.onPlaybackRequested(lifecycleScope) { playerItems ->
            when (playerItems) {
                is PlayerViewModel.PlayerItemError -> bindPlayerItemsError(playerItems)
                is PlayerViewModel.PlayerItems -> bindPlayerItems(playerItems)
            }
        }

        if (!args.isOffline) {
            val episodeId: UUID = args.episodeId

            binding.checkButton.setOnClickListener {
                when (viewModel.played) {
                    true -> {
                        viewModel.markAsUnplayed(episodeId)
                        binding.checkButton.setTintColorAttribute(R.attr.colorOnSecondaryContainer, requireActivity().theme)
                    }
                    false -> {
                        viewModel.markAsPlayed(episodeId)
                        binding.checkButton.setTintColor(R.color.red, requireActivity().theme)
                    }
                }
            }

            binding.favoriteButton.setOnClickListener {
                when (viewModel.favorite) {
                    true -> {
                        viewModel.unmarkAsFavorite(episodeId)
                        binding.favoriteButton.setImageResource(R.drawable.ic_heart)
                        binding.favoriteButton.setTintColorAttribute(R.attr.colorOnSecondaryContainer, requireActivity().theme)
                    }
                    false -> {
                        viewModel.markAsFavorite(episodeId)
                        binding.favoriteButton.setImageResource(R.drawable.ic_heart_filled)
                        binding.favoriteButton.setTintColor(R.color.red, requireActivity().theme)
                    }
                }
            }

            binding.downloadButton.setOnClickListener {
                binding.downloadButton.isEnabled = false
                viewModel.loadDownloadRequestItem(episodeId)
                binding.downloadButton.setTintColor(R.color.red, requireActivity().theme)
            }

            viewModel.loadEpisode(episodeId)
        } else {
            val playerItem = args.playerItem!!
            viewModel.loadEpisode(playerItem)

            binding.deleteButton.isVisible = true

            binding.deleteButton.setOnClickListener {
                viewModel.deleteEpisode()
                dismiss()
                findNavController().navigate(R.id.downloadFragment)
            }

            binding.checkButton.isVisible = false
            binding.favoriteButton.isVisible = false
            binding.downloadButtonWrapper.isVisible = false
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        dialog?.let {
            val sheet = it as BottomSheetDialog
            sheet.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    private fun bindUiStateNormal(uiState: EpisodeBottomSheetViewModel.UiState.Normal) {
        uiState.apply {
            if (episode.userData?.playedPercentage != null) {
                binding.progressBar.layoutParams.width = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    (episode.userData?.playedPercentage?.times(1.26))!!.toFloat(),
                    context?.resources?.displayMetrics
                ).toInt()
                binding.progressBar.isVisible = true
            }

            binding.playButton.isEnabled = available
            binding.playButton.alpha = if (!available) 0.5F else 1.0F

            // Check icon
            when (played) {
                true -> binding.checkButton.setTintColor(R.color.red, requireActivity().theme)
                false -> binding.checkButton.setTintColorAttribute(R.attr.colorOnSecondaryContainer, requireActivity().theme)
            }

            // Favorite icon
            val favoriteDrawable = when (favorite) {
                true -> R.drawable.ic_heart_filled
                false -> R.drawable.ic_heart
            }
            binding.favoriteButton.setImageResource(favoriteDrawable)
            if (favorite) binding.favoriteButton.setTintColor(R.color.red, requireActivity().theme)

            when (canDownload) {
                true -> {
                    binding.downloadButtonWrapper.isVisible = true
                    binding.downloadButton.isEnabled = !downloaded

                    if (downloaded) binding.downloadButton.setTintColor(R.color.red, requireActivity().theme)
                }
                false -> {
                    binding.downloadButtonWrapper.isVisible = false
                }
            }


            binding.episodeName.text = String.format(
                getString(R.string.episode_name_extended),
                episode.parentIndexNumber,
                episode.indexNumber,
                episode.name
            )
            binding.overview.text = episode.overview
            binding.year.text = dateString
            binding.playtime.text = runTime
            binding.communityRating.isVisible = episode.communityRating != null
            binding.communityRating.text = episode.communityRating.toString()
            binding.missingIcon.isVisible = episode.locationType == LocationType.VIRTUAL
            bindBaseItemImage(binding.episodeImage, episode)
        }
        binding.loadingIndicator.isVisible = false
    }

    private fun bindUiStateLoading() {
        binding.loadingIndicator.isVisible = true
    }

    private fun bindUiStateError(uiState: EpisodeBottomSheetViewModel.UiState.Error) {
        binding.loadingIndicator.isVisible = false
        binding.overview.text = uiState.error.message
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
        Timber.e(error.error.message)

        binding.playerItemsError.isVisible = true
        binding.playButton.setImageDrawable(
            ContextCompat.getDrawable(
                requireActivity(),
                R.drawable.ic_play
            )
        )
        binding.progressCircular.visibility = View.INVISIBLE
        binding.playerItemsErrorDetails.setOnClickListener {
            ErrorDialogFragment(error.error).show(parentFragmentManager, "errordialog")
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