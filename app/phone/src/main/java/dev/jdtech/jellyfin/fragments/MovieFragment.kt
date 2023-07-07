package dev.jdtech.jellyfin.fragments

import android.app.DownloadManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.AppPreferences
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.adapters.PersonListAdapter
import dev.jdtech.jellyfin.bindItemBackdropImage
import dev.jdtech.jellyfin.databinding.FragmentMovieBinding
import dev.jdtech.jellyfin.dialogs.ErrorDialogFragment
import dev.jdtech.jellyfin.dialogs.getStorageSelectionDialog
import dev.jdtech.jellyfin.dialogs.getVideoVersionDialog
import dev.jdtech.jellyfin.models.AudioCodec
import dev.jdtech.jellyfin.models.DisplayProfile
import dev.jdtech.jellyfin.models.FindroidSourceType
import dev.jdtech.jellyfin.models.PlayerItem
import dev.jdtech.jellyfin.models.UiText
import dev.jdtech.jellyfin.models.isDownloaded
import dev.jdtech.jellyfin.models.isDownloading
import dev.jdtech.jellyfin.utils.checkIfLoginRequired
import dev.jdtech.jellyfin.utils.setIconTintColorAttribute
import dev.jdtech.jellyfin.viewmodels.MovieViewModel
import dev.jdtech.jellyfin.viewmodels.PlayerViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import dev.jdtech.jellyfin.core.R as CoreR

@AndroidEntryPoint
class MovieFragment : Fragment() {
    private lateinit var binding: FragmentMovieBinding
    private val viewModel: MovieViewModel by viewModels()
    private val playerViewModel: PlayerViewModel by viewModels()
    private val args: MovieFragmentArgs by navArgs()

    private lateinit var errorDialog: ErrorDialogFragment
    private lateinit var downloadPreparingDialog: AlertDialog

    @Inject
    lateinit var appPreferences: AppPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentMovieBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { uiState ->
                        Timber.d("$uiState")
                        when (uiState) {
                            is MovieViewModel.UiState.Normal -> bindUiStateNormal(uiState)
                            is MovieViewModel.UiState.Loading -> bindUiStateLoading()
                            is MovieViewModel.UiState.Error -> bindUiStateError(uiState)
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
                                binding.itemActions.downloadButton.setIconResource(android.R.color.transparent)
                                binding.itemActions.progressDownload.isIndeterminate = true
                                binding.itemActions.progressDownload.isVisible = true
                            }
                            DownloadManager.STATUS_RUNNING -> {
                                binding.itemActions.downloadButton.setIconResource(android.R.color.transparent)
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

        binding.errorLayout.errorRetryButton.setOnClickListener {
            viewModel.loadData(args.itemId)
        }

        binding.errorLayout.errorDetailsButton.setOnClickListener {
            errorDialog.show(parentFragmentManager, ErrorDialogFragment.TAG)
        }

        playerViewModel.onPlaybackRequested(lifecycleScope) { playerItems ->
            when (playerItems) {
                is PlayerViewModel.PlayerItemError -> bindPlayerItemsError(playerItems)
                is PlayerViewModel.PlayerItems -> bindPlayerItems(playerItems)
            }
        }

        binding.itemActions.playButton.setOnClickListener {
            binding.itemActions.playButton.isEnabled = false
            binding.itemActions.playButton.setIconResource(android.R.color.transparent)
            binding.itemActions.progressPlay.isVisible = true
            if (viewModel.item.sources.filter { it.type == FindroidSourceType.REMOTE }.size > 1) {
                val dialog = getVideoVersionDialog(
                    requireContext(),
                    viewModel.item,
                    onItemSelected = {
                        playerViewModel.loadPlayerItems(viewModel.item, it)
                    },
                    onCancel = {
                        playButtonNormal()
                    },
                )
                dialog.show()
                return@setOnClickListener
            }
            playerViewModel.loadPlayerItems(viewModel.item)
        }

        binding.itemActions.trailerButton.setOnClickListener {
            viewModel.item.trailer.let { trailerUri ->
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(trailerUri),
                )
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), e.localizedMessage, Toast.LENGTH_SHORT).show()
                }
            }
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
                viewModel.deleteItem()
                binding.itemActions.downloadButton.setIconResource(CoreR.drawable.ic_download)
            } else if (viewModel.item.isDownloading()) {
                createCancelDialog()
            } else {
                binding.itemActions.downloadButton.setIconResource(android.R.color.transparent)
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

        binding.peopleRecyclerView.adapter = PersonListAdapter { person ->
            navigateToPersonDetail(person.id)
        }
    }

    override fun onResume() {
        super.onResume()

        viewModel.loadData(args.itemId)
    }

    private fun bindUiStateNormal(uiState: MovieViewModel.UiState.Normal) {
        uiState.apply {
            val size = item.sources.getOrNull(0)?.size?.let {
                Formatter.formatFileSize(requireContext(), it)
            }
            val canDownload =
                item.canDownload && item.sources.any { it.type == FindroidSourceType.REMOTE }
            val canDelete = item.sources.any { it.type == FindroidSourceType.LOCAL }

            binding.originalTitle.isVisible = item.originalTitle != item.name
            if (item.trailer != null) {
                binding.itemActions.trailerButton.isVisible = true
            }
            binding.communityRating.isVisible = item.communityRating != null
            binding.actors.isVisible = actors.isNotEmpty()

            binding.itemActions.playButton.isEnabled = item.canPlay && item.sources.isNotEmpty()
            binding.itemActions.checkButton.isEnabled = true
            binding.itemActions.favoriteButton.isEnabled = true

            bindCheckButtonState(item.played)

            bindFavoriteButtonState(item.favorite)

            if (item.isDownloaded()) {
                binding.itemActions.downloadButton.setIconResource(CoreR.drawable.ic_trash)
            }

            when (canDownload || canDelete) {
                true -> binding.itemActions.downloadButton.isVisible = true
                false -> binding.itemActions.downloadButton.isVisible = false
            }

            binding.name.text = item.name
            binding.originalTitle.text = item.originalTitle
            if (dateString.isEmpty()) {
                binding.year.isVisible = false
            } else {
                binding.year.text = dateString
            }
            if (runTime.isEmpty()) {
                binding.playtime.isVisible = false
            } else {
                binding.playtime.text = runTime
            }
            binding.officialRating.text = item.officialRating
            binding.communityRating.text = item.communityRating.toString()

            videoMetadata.let {
                with(binding) {
                    videoMetaChips.isVisible = true
                    audioChannelChip.text = it.audioChannels.firstOrNull()?.raw
                    resChip.text = it.resolution.firstOrNull()?.raw
                    audioChannelChip.isVisible = it.audioChannels.isNotEmpty()
                    resChip.isVisible = it.resolution.isNotEmpty()

                    it.displayProfiles.firstOrNull()?.apply {
                        videoProfileChip.text = this.raw
                        videoProfileChip.isVisible = when (this) {
                            DisplayProfile.HDR,
                            DisplayProfile.HDR10,
                            DisplayProfile.HLG,
                            -> {
                                videoProfileChip.chipStartPadding = .0f
                                true
                            }

                            DisplayProfile.DOLBY_VISION -> {
                                videoProfileChip.isChipIconVisible = true
                                true
                            }

                            else -> false
                        }
                    }

                    audioCodecChip.text = when (val codec = it.audioCodecs.firstOrNull()) {
                        AudioCodec.AC3, AudioCodec.EAC3, AudioCodec.TRUEHD -> {
                            audioCodecChip.isVisible = true
                            if (it.isAtmos.firstOrNull() == true) {
                                "${codec.raw} | Atmos"
                            } else {
                                codec.raw
                            }
                        }

                        AudioCodec.DTS -> {
                            audioCodecChip.apply {
                                isVisible = true
                                isChipIconVisible = false
                                chipStartPadding = .0f
                            }
                            codec.raw
                        }

                        else -> {
                            audioCodecChip.isVisible = false
                            null
                        }
                    }
                }
            }

            binding.subsChip.isVisible = subtitleString.isNotEmpty()

            if (appPreferences.displayExtraInfo) {
                binding.info.video.text = videoString
                binding.info.videoGroup.isVisible = videoString.isNotEmpty()
                binding.info.audio.text = audioString
                binding.info.audioGroup.isVisible = audioString.isNotEmpty()
                binding.info.subtitles.text = subtitleString
                binding.info.subtitlesGroup.isVisible = subtitleString.isNotEmpty()
                size?.let { binding.info.size.text = it }
                binding.info.sizeGroup.isVisible = size != null
            }

            binding.info.description.text = item.overview
            binding.info.genres.text = genresString
            binding.info.genresGroup.isVisible = item.genres.isNotEmpty()
            binding.info.director.text = director?.name
            binding.info.directorGroup.isVisible = director != null
            binding.info.writers.text = writersString
            binding.info.writersGroup.isVisible = writers.isNotEmpty()

            val actorsAdapter = binding.peopleRecyclerView.adapter as PersonListAdapter
            actorsAdapter.submitList(actors)
            bindItemBackdropImage(binding.itemBanner, item)
        }
        binding.loadingIndicator.isVisible = false
        binding.mediaInfoScrollview.isVisible = true
        binding.errorLayout.errorPanel.isVisible = false
    }

    private fun bindUiStateLoading() {
        binding.loadingIndicator.isVisible = true
        binding.errorLayout.errorPanel.isVisible = false
    }

    private fun bindUiStateError(uiState: MovieViewModel.UiState.Error) {
        errorDialog = ErrorDialogFragment.newInstance(uiState.error)
        binding.loadingIndicator.isVisible = false
        binding.mediaInfoScrollview.isVisible = false
        binding.errorLayout.errorPanel.isVisible = true
        checkIfLoginRequired(uiState.error.message)
    }

    private fun bindCheckButtonState(played: Boolean) {
        when (played) {
            true -> binding.itemActions.checkButton.setIconTintResource(CoreR.color.red)
            false -> binding.itemActions.checkButton.setIconTintColorAttribute(
                com.google.android.material.R.attr.colorOnSecondaryContainer,
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
            true -> binding.itemActions.favoriteButton.setIconTintResource(CoreR.color.red)
            false -> binding.itemActions.favoriteButton.setIconTintColorAttribute(
                com.google.android.material.R.attr.colorOnSecondaryContainer,
                requireActivity().theme,
            )
        }
    }

    private fun bindPlayerItems(items: PlayerViewModel.PlayerItems) {
        navigateToPlayerActivity(items.items.toTypedArray())
        binding.itemActions.playButton.setIconResource(CoreR.drawable.ic_play)
        binding.itemActions.progressPlay.visibility = View.INVISIBLE
    }

    private fun bindPlayerItemsError(error: PlayerViewModel.PlayerItemError) {
        Timber.e(error.error.message)
        binding.playerItemsError.visibility = View.VISIBLE
        playButtonNormal()
        binding.playerItemsErrorDetails.setOnClickListener {
            ErrorDialogFragment.newInstance(error.error)
                .show(parentFragmentManager, ErrorDialogFragment.TAG)
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
            MovieFragmentDirections.actionMovieFragmentToPlayerActivity(
                playerItems,
            ),
        )
    }

    private fun navigateToPersonDetail(personId: UUID) {
        findNavController().navigate(
            MovieFragmentDirections.actionMovieFragmentToPersonDetailFragment(personId),
        )
    }
}
