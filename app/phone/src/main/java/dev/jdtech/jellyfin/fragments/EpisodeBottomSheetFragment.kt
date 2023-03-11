package dev.jdtech.jellyfin.fragments

import android.app.DownloadManager
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
import dev.jdtech.jellyfin.bindCardItemImage
import dev.jdtech.jellyfin.databinding.EpisodeBottomSheetBinding
import dev.jdtech.jellyfin.dialogs.ErrorDialogFragment
import dev.jdtech.jellyfin.dialogs.getVideoVersionDialog
import dev.jdtech.jellyfin.models.FindroidSourceType
import dev.jdtech.jellyfin.models.PlayerItem
import dev.jdtech.jellyfin.models.isDownloaded
import dev.jdtech.jellyfin.utils.setTintColor
import dev.jdtech.jellyfin.utils.setTintColorAttribute
import dev.jdtech.jellyfin.viewmodels.EpisodeBottomSheetViewModel
import dev.jdtech.jellyfin.viewmodels.PlayerViewModel
import java.text.DateFormat
import java.time.ZoneOffset
import java.util.Date
import java.util.UUID
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.DateTime
import timber.log.Timber

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
            playerViewModel.loadPlayerItems(viewModel.item)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    Timber.d("$uiState")
                    when (uiState) {
                        is EpisodeBottomSheetViewModel.UiState.Normal -> bindUiStateNormal(uiState)
                        is EpisodeBottomSheetViewModel.UiState.Loading -> bindUiStateLoading()
                        is EpisodeBottomSheetViewModel.UiState.Error -> bindUiStateError(uiState)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.downloadStatus.collect { (status, progress) ->
                    when (status) {
                        0 -> Unit
                        DownloadManager.STATUS_PENDING -> {
                            binding.downloadButton.isEnabled = false
                            binding.downloadButton.setImageResource(android.R.color.transparent)
                            binding.progressDownload.isIndeterminate = true
                            binding.progressDownload.isVisible = true
                        }
                        DownloadManager.STATUS_RUNNING -> {
                            binding.downloadButton.isEnabled = false
                            binding.downloadButton.setImageResource(android.R.color.transparent)
                            binding.progressDownload.isIndeterminate = false
                            binding.progressDownload.isVisible = true
                            binding.progressDownload.setProgressCompat(progress, true)
                        }
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            binding.downloadButton.setImageResource(R.drawable.ic_trash)
                            binding.progressDownload.isVisible = false
                            binding.downloadButton.isEnabled = true
                        }
                        else -> {
                            binding.progressDownload.isVisible = false
                            binding.downloadButton.setImageResource(R.drawable.ic_download)
                            binding.downloadButton.isEnabled = true
                        }
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

        binding.seriesName.setOnClickListener {
            navigateToSeries(viewModel.item.seriesId, viewModel.item.seriesName)
        }

        binding.checkButton.setOnClickListener {
            viewModel.togglePlayed()
        }

        binding.favoriteButton.setOnClickListener {
            viewModel.toggleFavorite()
        }

        binding.downloadButton.setOnClickListener {
            if (viewModel.item.isDownloaded()) {
                viewModel.deleteEpisode()
                binding.downloadButton.setImageResource(R.drawable.ic_download)
            } else {
                if (viewModel.item.sources.size > 1) {
                    val dialog = getVideoVersionDialog(requireContext(), viewModel.item) {
                        viewModel.download(it)
                    }
                    dialog.show()
                    return@setOnClickListener
                }
                viewModel.download()
            }
        }

        viewModel.loadEpisode(args.episodeId)

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
            val canDownload = episode.canDownload && episode.sources.any { it.type == FindroidSourceType.REMOTE }
            val canDelete = episode.sources.any { it.type == FindroidSourceType.LOCAL }

            if (episode.playbackPositionTicks > 0) {
                binding.progressBar.layoutParams.width = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    (episode.playbackPositionTicks.div(episode.runtimeTicks).times(1.26)).toFloat(),
                    context?.resources?.displayMetrics
                ).toInt()
                binding.progressBar.isVisible = true
            }

            val canPlay = episode.canPlay && episode.sources.isNotEmpty()
            binding.playButton.isEnabled = canPlay
            binding.playButton.alpha = if (!canPlay) 0.5F else 1.0F

            // Check icon
            when (episode.played) {
                true -> binding.checkButton.setTintColor(R.color.red, requireActivity().theme)
                false -> binding.checkButton.setTintColorAttribute(R.attr.colorOnSecondaryContainer, requireActivity().theme)
            }

            // Favorite icon
            val favoriteDrawable = when (episode.favorite) {
                true -> R.drawable.ic_heart_filled
                false -> R.drawable.ic_heart
            }
            binding.favoriteButton.setImageResource(favoriteDrawable)
            when (episode.favorite) {
                true -> binding.favoriteButton.setTintColor(R.color.red, requireActivity().theme)
                false -> binding.favoriteButton.setTintColorAttribute(R.attr.colorOnSecondaryContainer, requireActivity().theme)
            }

            if (episode.isDownloaded()) {
                binding.downloadButton.setImageResource(R.drawable.ic_trash)
            }

            when (canDownload || canDelete) {
                true -> binding.downloadButton.isVisible = true
                false -> binding.downloadButton.isVisible = false
            }

            binding.episodeName.text = getString(
                R.string.episode_name_extended,
                episode.parentIndexNumber,
                episode.indexNumber,
                episode.name
            )
            binding.seriesName.text = episode.seriesName
            binding.overview.text = episode.overview
            binding.year.text = formatDateTime(episode.premiereDate)
            binding.playtime.text = getString(R.string.runtime_minutes, episode.runtimeTicks.div(600000000))
            binding.communityRating.isVisible = episode.communityRating != null
            binding.communityRating.text = episode.communityRating.toString()
            binding.missingIcon.isVisible = false

            bindCardItemImage(binding.episodeImage, episode)
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
            ErrorDialogFragment.newInstance(error.error).show(parentFragmentManager, ErrorDialogFragment.TAG)
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

    private fun navigateToSeries(id: UUID, name: String) {
        findNavController().navigate(
            EpisodeBottomSheetFragmentDirections.actionEpisodeBottomSheetFragmentToShowFragment(
                itemId = id,
                itemName = name
            )
        )
    }

    private fun formatDateTime(datetime: DateTime?): String {
        if (datetime == null) return ""
        val instant = datetime.toInstant(ZoneOffset.UTC)
        val date = Date.from(instant)
        return DateFormat.getDateInstance(DateFormat.SHORT).format(date)
    }
}
