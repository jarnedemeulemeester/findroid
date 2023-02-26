package dev.jdtech.jellyfin.fragments

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.AppPreferences
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.adapters.PersonListAdapter
import dev.jdtech.jellyfin.adapters.ViewItemListAdapter
import dev.jdtech.jellyfin.bindCardItemImage
import dev.jdtech.jellyfin.bindItemBackdropImage
import dev.jdtech.jellyfin.databinding.FragmentMediaInfoBinding
import dev.jdtech.jellyfin.dialogs.ErrorDialogFragment
import dev.jdtech.jellyfin.dialogs.VideoVersionDialogFragment
import dev.jdtech.jellyfin.models.AudioCodec
import dev.jdtech.jellyfin.models.DisplayProfile
import dev.jdtech.jellyfin.models.JellyfinItem
import dev.jdtech.jellyfin.models.JellyfinSeasonItem
import dev.jdtech.jellyfin.models.JellyfinSourceType
import dev.jdtech.jellyfin.models.PlayerItem
import dev.jdtech.jellyfin.utils.checkIfLoginRequired
import dev.jdtech.jellyfin.utils.setTintColor
import dev.jdtech.jellyfin.utils.setTintColorAttribute
import dev.jdtech.jellyfin.viewmodels.MediaInfoViewModel
import dev.jdtech.jellyfin.viewmodels.PlayerViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.BaseItemKind
import timber.log.Timber

// TODO move Shows to a seperate fragment

@AndroidEntryPoint
class MediaInfoFragment : Fragment() {

    private lateinit var binding: FragmentMediaInfoBinding
    private val viewModel: MediaInfoViewModel by viewModels()
    private val playerViewModel: PlayerViewModel by viewModels()
    private val args: MediaInfoFragmentArgs by navArgs()

    private lateinit var errorDialog: ErrorDialogFragment

    @Inject
    lateinit var appPreferences: AppPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMediaInfoBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    Timber.d("$uiState")
                    when (uiState) {
                        is MediaInfoViewModel.UiState.Normal -> bindUiStateNormal(uiState)
                        is MediaInfoViewModel.UiState.Loading -> bindUiStateLoading()
                        is MediaInfoViewModel.UiState.Error -> bindUiStateError(uiState)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.loadData(args.itemId, args.itemType)
            }
        }

        if (args.itemType != BaseItemKind.MOVIE) {
            binding.downloadButton.visibility = View.GONE
        }

        binding.errorLayout.errorRetryButton.setOnClickListener {
            viewModel.loadData(args.itemId, args.itemType)
        }

        playerViewModel.onPlaybackRequested(lifecycleScope) { playerItems ->
            when (playerItems) {
                is PlayerViewModel.PlayerItemError -> bindPlayerItemsError(playerItems)
                is PlayerViewModel.PlayerItems -> bindPlayerItems(playerItems)
            }
        }

//        binding.trailerButton.setOnClickListener {
//            if (viewModel.item?.remoteTrailers.isNullOrEmpty()) return@setOnClickListener
//            val intent = Intent(
//                Intent.ACTION_VIEW,
//                Uri.parse(viewModel.item?.remoteTrailers?.get(0)?.url)
//            )
//            startActivity(intent)
//        }

        binding.nextUp.setOnClickListener {
            navigateToEpisodeBottomSheetFragment(viewModel.nextUp!!)
        }

        binding.seasonsRecyclerView.adapter =
            ViewItemListAdapter(
                ViewItemListAdapter.OnClickListener { season ->
                    if (season is JellyfinSeasonItem) navigateToSeasonFragment(season)
                },
                fixedWidth = true
            )
        binding.peopleRecyclerView.adapter = PersonListAdapter { person ->
            navigateToPersonDetail(person.id)
        }

        binding.playButton.setOnClickListener {
            binding.playButton.setImageResource(android.R.color.transparent)
            binding.progressCircular.isVisible = true
            playerViewModel.loadPlayerItems(viewModel.item) {
                VideoVersionDialogFragment(viewModel.item, playerViewModel).show(
                    parentFragmentManager,
                    "videoversiondialog"
                )
            }
        }

        binding.errorLayout.errorRetryButton.setOnClickListener {
            viewModel.loadData(args.itemId, args.itemType)
        }

        binding.errorLayout.errorDetailsButton.setOnClickListener {
            errorDialog.show(parentFragmentManager, ErrorDialogFragment.TAG)
        }

        binding.checkButton.setOnClickListener {
            viewModel.togglePlayed()
        }

        binding.favoriteButton.setOnClickListener {
            viewModel.toggleFavorite()
        }

        binding.downloadButton.setOnClickListener {
            binding.downloadButton.isEnabled = false
            viewModel.download()
            binding.downloadButton.imageTintList = ColorStateList.valueOf(
                resources.getColor(
                    R.color.red,
                    requireActivity().theme
                )
            )
        }
    }

    private fun bindUiStateNormal(uiState: MediaInfoViewModel.UiState.Normal) {
        uiState.apply {
            val downloaded = item.sources.any { it.type == JellyfinSourceType.LOCAL }
            val canDownload = item.canDownload && item.sources.any { it.type == JellyfinSourceType.REMOTE }

            binding.originalTitle.isVisible = item.originalTitle != item.name
//            if (item.remoteTrailers.isNullOrEmpty()) {
//                binding.trailerButton.isVisible = false
//            }
            binding.communityRating.isVisible = item.communityRating != null
            binding.actors.isVisible = actors.isNotEmpty()

            val canPlay = item.canPlay && item.sources.isNotEmpty()
            binding.playButton.isEnabled = canPlay
            binding.playButton.alpha = if (!canPlay) 0.5F else 1.0F

            // Check icon
            when (item.played) {
                true -> binding.checkButton.setTintColor(R.color.red, requireActivity().theme)
                false -> binding.checkButton.setTintColorAttribute(
                    R.attr.colorOnSecondaryContainer,
                    requireActivity().theme
                )
            }

            // Favorite icon
            val favoriteDrawable = when (item.favorite) {
                true -> R.drawable.ic_heart_filled
                false -> R.drawable.ic_heart
            }
            binding.favoriteButton.setImageResource(favoriteDrawable)
            when (item.favorite) {
                true -> binding.favoriteButton.setTintColor(R.color.red, requireActivity().theme)
                false -> binding.favoriteButton.setTintColorAttribute(R.attr.colorOnSecondaryContainer, requireActivity().theme)
            }

            when (canDownload) {
                true -> {
                    binding.downloadButton.isVisible = true
                    binding.downloadButton.isEnabled = !downloaded

                    if (downloaded) binding.downloadButton.setTintColor(
                        R.color.red,
                        requireActivity().theme
                    )
                }

                false -> {
                    binding.downloadButton.isVisible = false
                }
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
            binding.genresLayout.isVisible = item.genres.isNotEmpty()
            binding.genres.text = genresString
            binding.videoMeta.text = videoString
            binding.audio.text = audioString
            binding.subtitles.text = subtitleString
            binding.subsChip.isVisible = subtitleString.isNotEmpty()

            if (appPreferences.displayExtraInfo) {
                binding.subtitlesLayout.isVisible = subtitleString.isNotEmpty()
                binding.videoMetaLayout.isVisible = videoString.isNotEmpty()
                binding.audioLayout.isVisible = audioString.isNotEmpty()
            }

            videoMetadata?.let {
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
                            DisplayProfile.HLG -> {
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
                            } else codec.raw
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
            binding.directorLayout.isVisible = director != null
            binding.director.text = director?.name
            binding.writersLayout.isVisible = writers.isNotEmpty()
            binding.writers.text = writersString
            binding.description.text = item.overview
            binding.nextUpLayout.isVisible = nextUp != null
            binding.nextUpName.text = getString(
                R.string.episode_name_extended,
                nextUp?.parentIndexNumber,
                nextUp?.indexNumber,
                nextUp?.name
            )
            binding.seasonsLayout.isVisible = seasons.isNotEmpty()
            val seasonsAdapter = binding.seasonsRecyclerView.adapter as ViewItemListAdapter
            seasonsAdapter.submitList(seasons)
            val actorsAdapter = binding.peopleRecyclerView.adapter as PersonListAdapter
            actorsAdapter.submitList(actors)
            bindItemBackdropImage(binding.itemBanner, item)
            if (nextUp != null) bindCardItemImage(binding.nextUpImage, nextUp!!)
        }
        binding.loadingIndicator.isVisible = false
        binding.mediaInfoScrollview.isVisible = true
        binding.errorLayout.errorPanel.isVisible = false
    }

    private fun bindUiStateLoading() {
        binding.loadingIndicator.isVisible = true
        binding.errorLayout.errorPanel.isVisible = false
    }

    private fun bindUiStateError(uiState: MediaInfoViewModel.UiState.Error) {
        errorDialog = ErrorDialogFragment.newInstance(uiState.error)
        binding.loadingIndicator.isVisible = false
        binding.mediaInfoScrollview.isVisible = false
        binding.errorLayout.errorPanel.isVisible = true
        checkIfLoginRequired(uiState.error.message)
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
        binding.playerItemsError.visibility = View.VISIBLE
        binding.playButton.setImageDrawable(
            ContextCompat.getDrawable(
                requireActivity(),
                R.drawable.ic_play
            )
        )
        binding.progressCircular.visibility = View.INVISIBLE
        binding.playerItemsErrorDetails.setOnClickListener {
            ErrorDialogFragment.newInstance(error.error)
                .show(parentFragmentManager, ErrorDialogFragment.TAG)
        }
    }

    private fun navigateToEpisodeBottomSheetFragment(episode: JellyfinItem) {
        findNavController().navigate(
            MediaInfoFragmentDirections.actionMediaInfoFragmentToEpisodeBottomSheetFragment(
                episode.id
            )
        )
    }

    private fun navigateToSeasonFragment(season: JellyfinSeasonItem) {
        findNavController().navigate(
            MediaInfoFragmentDirections.actionMediaInfoFragmentToSeasonFragment(
                season.seriesId,
                season.id,
                season.seriesName,
                season.name
            )
        )
    }

    private fun navigateToPlayerActivity(
        playerItems: Array<PlayerItem>,
    ) {
        findNavController().navigate(
            MediaInfoFragmentDirections.actionMediaInfoFragmentToPlayerActivity(
                playerItems
            )
        )
    }

    private fun navigateToPersonDetail(personId: UUID) {
        findNavController().navigate(
            MediaInfoFragmentDirections.actionMediaInfoFragmentToPersonDetailFragment(personId)
        )
    }
}
