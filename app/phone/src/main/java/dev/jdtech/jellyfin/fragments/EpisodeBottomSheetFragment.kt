package dev.jdtech.jellyfin.fragments

import android.app.DownloadManager
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.bindCardItemImage
import dev.jdtech.jellyfin.databinding.EpisodeBottomSheetBinding
import dev.jdtech.jellyfin.dialogs.ErrorDialogFragment
import dev.jdtech.jellyfin.dialogs.getStorageSelectionDialog
import dev.jdtech.jellyfin.dialogs.getVideoVersionDialog
import dev.jdtech.jellyfin.models.FindroidSourceType
import dev.jdtech.jellyfin.models.PlayerItem
import dev.jdtech.jellyfin.models.UiText
import dev.jdtech.jellyfin.models.isDownloaded
import dev.jdtech.jellyfin.models.isDownloading
import dev.jdtech.jellyfin.utils.setIconTintColorAttribute
import dev.jdtech.jellyfin.viewmodels.EpisodeBottomSheetViewModel
import dev.jdtech.jellyfin.viewmodels.PlayerViewModel
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.DateTime
import timber.log.Timber
import java.text.DateFormat
import java.time.ZoneOffset
import java.util.Date
import java.util.UUID
import android.R as AndroidR
import com.google.android.material.R as MaterialR
import dev.jdtech.jellyfin.core.R as CoreR

@AndroidEntryPoint
class EpisodeBottomSheetFragment : BottomSheetDialogFragment() {
    private val args: EpisodeBottomSheetFragmentArgs by navArgs()

    private lateinit var binding: EpisodeBottomSheetBinding
    private val viewModel: EpisodeBottomSheetViewModel by viewModels()
    private val playerViewModel: PlayerViewModel by viewModels()

    private lateinit var downloadPreparingDialog: AlertDialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = EpisodeBottomSheetBinding.inflate(inflater, container, false)

        binding.itemActions.playButton.setOnClickListener {
            binding.itemActions.playButton.isEnabled = false
            binding.itemActions.playButton.setIconResource(AndroidR.color.transparent)
            binding.itemActions.progressPlay.isVisible = true
            playerViewModel.loadPlayerItems(viewModel.item)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { uiState ->
                        Timber.d("$uiState")
                        when (uiState) {
                            is EpisodeBottomSheetViewModel.UiState.Normal -> bindUiStateNormal(uiState)
                            is EpisodeBottomSheetViewModel.UiState.Loading -> bindUiStateLoading()
                            is EpisodeBottomSheetViewModel.UiState.Error -> bindUiStateError(uiState)
                        }
                    }
                }

                launch {
                    viewModel.downloadStatus.collect { (status, progress) ->
                        when (status) {
                            10 -> {
                                downloadPreparingDialog.dismiss()
                            }
                            DownloadManager.STATUS_PENDING -> {
                                binding.itemActions.downloadButton.setIconResource(AndroidR.color.transparent)
                                binding.itemActions.progressDownload.isIndeterminate = true
                                binding.itemActions.progressDownload.isVisible = true
                            }

                            DownloadManager.STATUS_RUNNING -> {
                                binding.itemActions.downloadButton.setIconResource(AndroidR.color.transparent)
                                binding.itemActions.progressDownload.isVisible = true
                                if (progress < 5) {
                                    binding.itemActions.progressDownload.isIndeterminate = true
                                } else {
                                    binding.itemActions.progressDownload.isIndeterminate = false
                                    binding.itemActions.progressDownload.setProgressCompat(progress, true)
                                }
                            }
                            DownloadManager.STATUS_SUCCESSFUL -> {
                                binding.itemActions.downloadButton.setIconResource(CoreR.drawable.ic_trash)
                                binding.itemActions.progressDownload.isVisible = false
                            }
                            else -> {
                                binding.itemActions.progressDownload.isVisible = false
                                binding.itemActions.downloadButton.setIconResource(CoreR.drawable.ic_download)
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

        binding.itemActions.checkButton.setOnClickListener {
            val played = viewModel.togglePlayed()
            bindCheckButtonState(played)
        }

        binding.itemActions.favoriteButton.setOnClickListener {
            val favorite = viewModel.toggleFavorite()
            bindFavoriteButtonState(favorite)
        }

        binding.itemActions.downloadButton.setOnClickListener {
            if (viewModel.item.isDownloaded()) {
                viewModel.deleteEpisode()
                binding.itemActions.downloadButton.setIconResource(CoreR.drawable.ic_download)
            } else if (viewModel.item.isDownloading()) {
                createCancelDialog()
            } else {
                binding.itemActions.downloadButton.setIconResource(AndroidR.color.transparent)
                binding.itemActions.progressDownload.isIndeterminate = true
                binding.itemActions.progressDownload.isVisible = true
                if (requireContext().getExternalFilesDirs(null).filterNotNull().size > 1) {
                    val storageDialog = getStorageSelectionDialog(
                        requireContext(),
                        onItemSelected = { storageIndex ->
                            if (viewModel.item.sources.size > 1) {
                                val dialog = getVideoVersionDialog(
                                    requireContext(),
                                    viewModel.item,
                                    onItemSelected = { sourceIndex ->
                                        createDownloadPreparingDialog()
                                        viewModel.download(sourceIndex, storageIndex)
                                    },
                                    onCancel = {
                                        binding.itemActions.progressDownload.isVisible = false
                                        binding.itemActions.downloadButton.setIconResource(CoreR.drawable.ic_download)
                                    },
                                )
                                dialog.show()
                                return@getStorageSelectionDialog
                            }
                            createDownloadPreparingDialog()
                            viewModel.download(storageIndex = storageIndex)
                        },
                        onCancel = {
                            binding.itemActions.progressDownload.isVisible = false
                            binding.itemActions.downloadButton.setIconResource(CoreR.drawable.ic_download)
                        },
                    )
                    storageDialog.show()
                    return@setOnClickListener
                }
                if (viewModel.item.sources.size > 1) {
                    val dialog = getVideoVersionDialog(
                        requireContext(),
                        viewModel.item,
                        onItemSelected = { sourceIndex ->
                            createDownloadPreparingDialog()
                            viewModel.download(sourceIndex)
                        },
                        onCancel = {
                            binding.itemActions.progressDownload.isVisible = false
                            binding.itemActions.downloadButton.setIconResource(CoreR.drawable.ic_download)
                        },
                    )
                    dialog.show()
                    return@setOnClickListener
                }
                createDownloadPreparingDialog()
                viewModel.download()
            }
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        dialog?.let {
            val sheet = it as BottomSheetDialog
            sheet.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    override fun onResume() {
        super.onResume()

        viewModel.loadEpisode(args.episodeId)
    }

    private fun bindUiStateNormal(uiState: EpisodeBottomSheetViewModel.UiState.Normal) {
        uiState.apply {
            val canDownload = episode.canDownload && episode.sources.any { it.type == FindroidSourceType.REMOTE }
            val canDelete = episode.sources.any { it.type == FindroidSourceType.LOCAL }

            if (episode.playbackPositionTicks > 0) {
                binding.progressBar.layoutParams.width = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    (episode.playbackPositionTicks.div(episode.runtimeTicks).times(1.26)).toFloat(),
                    context?.resources?.displayMetrics,
                ).toInt()
                binding.progressBar.isVisible = true
            }

            binding.itemActions.playButton.isEnabled = episode.canPlay && episode.sources.isNotEmpty()
            binding.itemActions.checkButton.isEnabled = true
            binding.itemActions.favoriteButton.isEnabled = true

            bindCheckButtonState(episode.played)

            bindFavoriteButtonState(episode.favorite)

            if (episode.isDownloaded()) {
                binding.itemActions.downloadButton.setIconResource(CoreR.drawable.ic_trash)
            }

            when (canDownload || canDelete) {
                true -> binding.itemActions.downloadButton.isVisible = true
                false -> binding.itemActions.downloadButton.isVisible = false
            }

            binding.episodeName.text = if (episode.indexNumberEnd == null) {
                getString(
                    CoreR.string.episode_name_extended,
                    episode.parentIndexNumber,
                    episode.indexNumber,
                    episode.name,
                )
            } else {
                getString(
                    CoreR.string.episode_name_extended_with_end,
                    episode.parentIndexNumber,
                    episode.indexNumber,
                    episode.indexNumberEnd,
                    episode.name,
                )
            }

            binding.seriesName.text = episode.seriesName
            binding.overview.text = episode.overview
            binding.year.text = formatDateTime(episode.premiereDate)
            binding.playtime.text = getString(CoreR.string.runtime_minutes, episode.runtimeTicks.div(600000000))
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
        binding.itemActions.playButton.setIconResource(CoreR.drawable.ic_play)
        binding.itemActions.progressPlay.visibility = View.INVISIBLE
    }

    private fun bindCheckButtonState(played: Boolean) {
        when (played) {
            true -> binding.itemActions.checkButton.setIconTintResource(
                CoreR.color.red,
            )

            false -> binding.itemActions.checkButton.setIconTintColorAttribute(
                MaterialR.attr.colorOnSecondaryContainer,
                requireActivity().theme,
            )
        }
    }

    private fun bindFavoriteButtonState(favorite: Boolean) {
        val favoriteDrawable = when (favorite) {
            true -> CoreR.drawable.ic_heart_filled
            false -> CoreR.drawable.ic_heart
        }
        binding.itemActions.favoriteButton.setIconResource(favoriteDrawable)
        when (favorite) {
            true -> binding.itemActions.favoriteButton.setIconTintResource(
                CoreR.color.red,
            )

            false -> binding.itemActions.favoriteButton.setIconTintColorAttribute(
                MaterialR.attr.colorOnSecondaryContainer,
                requireActivity().theme,
            )
        }
    }

    private fun bindPlayerItemsError(error: PlayerViewModel.PlayerItemError) {
        Timber.e(error.error.message)
        binding.playerItemsError.isVisible = true
        playButtonNormal()
        binding.playerItemsErrorDetails.setOnClickListener {
            ErrorDialogFragment.newInstance(error.error).show(parentFragmentManager, ErrorDialogFragment.TAG)
        }
    }

    private fun playButtonNormal() {
        binding.itemActions.playButton.isEnabled = true
        binding.itemActions.playButton.setIconResource(CoreR.drawable.ic_play)
        binding.itemActions.progressPlay.visibility = View.INVISIBLE
    }

    private fun createErrorDialog(uiText: UiText) {
        val builder = MaterialAlertDialogBuilder(requireContext())
        builder
            .setTitle(CoreR.string.downloading_error)
            .setMessage(uiText.asString(requireContext().resources))
            .setPositiveButton(getString(CoreR.string.close)) { _, _ ->
            }
        builder.show()
        binding.itemActions.progressDownload.isVisible = false
        binding.itemActions.downloadButton.setIconResource(CoreR.drawable.ic_download)
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

    private fun createCancelDialog() {
        val builder = MaterialAlertDialogBuilder(requireContext())
        val dialog = builder
            .setTitle(CoreR.string.cancel_download)
            .setMessage(CoreR.string.cancel_download_message)
            .setPositiveButton(CoreR.string.stop_download) { _, _ ->
                viewModel.cancelDownload()
            }
            .setNegativeButton(CoreR.string.cancel) { _, _ ->
            }
            .create()
        dialog.show()
    }

    private fun navigateToPlayerActivity(
        playerItems: Array<PlayerItem>,
    ) {
        findNavController().navigate(
            EpisodeBottomSheetFragmentDirections.actionEpisodeBottomSheetFragmentToPlayerActivity(
                playerItems,
            ),
        )
    }

    private fun navigateToSeries(id: UUID, name: String) {
        findNavController().navigate(
            EpisodeBottomSheetFragmentDirections.actionEpisodeBottomSheetFragmentToShowFragment(
                itemId = id,
                itemName = name,
            ),
        )
    }

    private fun formatDateTime(datetime: DateTime?): String {
        if (datetime == null) return ""
        val instant = datetime.toInstant(ZoneOffset.UTC)
        val date = Date.from(instant)
        return DateFormat.getDateInstance(DateFormat.SHORT).format(date)
    }
}
