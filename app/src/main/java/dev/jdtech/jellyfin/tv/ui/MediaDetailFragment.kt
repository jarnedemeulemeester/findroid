package dev.jdtech.jellyfin.tv.ui

import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.adapters.PersonListAdapter
import dev.jdtech.jellyfin.adapters.ViewItemListAdapter
import dev.jdtech.jellyfin.bindBaseItemImage
import dev.jdtech.jellyfin.databinding.MediaDetailFragmentBinding
import dev.jdtech.jellyfin.dialogs.VideoVersionDialogFragment
import dev.jdtech.jellyfin.models.PlayerItem
import dev.jdtech.jellyfin.viewmodels.MediaInfoViewModel
import dev.jdtech.jellyfin.viewmodels.PlayerViewModel
import dev.jdtech.jellyfin.viewmodels.PlayerViewModel.PlayerItemError
import dev.jdtech.jellyfin.viewmodels.PlayerViewModel.PlayerItems
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
internal class MediaDetailFragment : Fragment() {

    private lateinit var binding: MediaDetailFragmentBinding

    private val viewModel: MediaInfoViewModel by viewModels()
    private val playerViewModel: PlayerViewModel by viewModels()

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
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.onUiState(viewLifecycleOwner.lifecycleScope) { uiState ->
                    Timber.d("$uiState")
                    when (uiState) {
                        is MediaInfoViewModel.UiState.Normal -> bindUiStateNormal(uiState)
                        is MediaInfoViewModel.UiState.Loading -> bindUiStateLoading()
                        is MediaInfoViewModel.UiState.Error -> bindUiStateError(uiState)
                    }
                }
            }
        }

        val seasonsAdapter = ViewItemListAdapter(
            fixedWidth = true,
            onClickListener = ViewItemListAdapter.OnClickListener {})

        binding.seasonsRow.gridView.adapter = seasonsAdapter
        binding.seasonsRow.gridView.verticalSpacing = 25

        val castAdapter = PersonListAdapter { person ->
            Toast.makeText(requireContext(), "Not yet implemented", Toast.LENGTH_SHORT).show()
        }

        binding.castRow.gridView.adapter = castAdapter
        binding.castRow.gridView.verticalSpacing = 25

        playerViewModel.onPlaybackRequested(lifecycleScope) { playerItems ->
            when (playerItems) {
                is PlayerItemError -> bindPlayerItemsError(playerItems)
                is PlayerItems -> bindPlayerItems(playerItems)
            }
        }

        binding.playButton.setOnClickListener {
            binding.playButton.setImageResource(android.R.color.transparent)
            binding.progressCircular.isVisible = true
            viewModel.item?.let { item ->
                playerViewModel.loadPlayerItems(item) {
                    VideoVersionDialogFragment(item, playerViewModel).show(
                        parentFragmentManager,
                        "videoversiondialog"
                    )
                }
            }
        }

        binding.trailerButton.setOnClickListener {
            if (viewModel.item?.remoteTrailers.isNullOrEmpty()) return@setOnClickListener
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(viewModel.item?.remoteTrailers?.get(0)?.url)
            )
            startActivity(intent)
        }

        binding.checkButton.setOnClickListener {
            when (viewModel.played) {
                true -> {
                    viewModel.markAsUnplayed(args.itemId)
                    val typedValue = TypedValue()
                    requireActivity().theme.resolveAttribute(R.attr.colorOnSecondaryContainer, typedValue, true)
                    binding.checkButton.imageTintList = ColorStateList.valueOf(
                        resources.getColor(
                            typedValue.resourceId,
                            requireActivity().theme
                        )
                    )
                }
                false -> {
                    viewModel.markAsPlayed(args.itemId)
                    binding.checkButton.imageTintList = ColorStateList.valueOf(
                        resources.getColor(
                            R.color.red,
                            requireActivity().theme
                        )
                    )
                }
            }
        }

        binding.favoriteButton.setOnClickListener {
            when (viewModel.favorite) {
                true -> {
                    viewModel.unmarkAsFavorite(args.itemId)
                    binding.favoriteButton.setImageResource(R.drawable.ic_heart)
                    val typedValue = TypedValue()
                    requireActivity().theme.resolveAttribute(R.attr.colorOnSecondaryContainer, typedValue, true)
                    binding.favoriteButton.imageTintList = ColorStateList.valueOf(
                        resources.getColor(
                            typedValue.resourceId,
                            requireActivity().theme
                        )
                    )
                }
                false -> {
                    viewModel.markAsFavorite(args.itemId)
                    binding.favoriteButton.setImageResource(R.drawable.ic_heart_filled)
                    binding.favoriteButton.imageTintList = ColorStateList.valueOf(
                        resources.getColor(
                            R.color.red,
                            requireActivity().theme
                        )
                    )
                }
            }
        }

        binding.backButton.setOnClickListener { activity?.onBackPressed() }
    }

    private fun bindUiStateNormal(uiState: MediaInfoViewModel.UiState.Normal) {
        uiState.apply {
            binding.seasonTitle.isVisible = seasons.isNotEmpty()
            val seasonsAdapter = binding.seasonsRow.gridView.adapter as ViewItemListAdapter
            seasonsAdapter.submitList(seasons)
            binding.castTitle.isVisible = actors.isNotEmpty()
            val actorsAdapter = binding.castRow.gridView.adapter as PersonListAdapter
            actorsAdapter.submitList(actors)

            // Check icon
            when (played) {
                true -> {
                    if (played) binding.checkButton.imageTintList = ColorStateList.valueOf(
                        resources.getColor(
                            R.color.red,
                            requireActivity().theme
                        )
                    )
                }
                false -> {
                    val typedValue = TypedValue()
                    requireActivity().theme.resolveAttribute(R.attr.colorOnSecondaryContainer, typedValue, true)
                    binding.checkButton.imageTintList = ColorStateList.valueOf(
                        resources.getColor(
                            typedValue.resourceId,
                            requireActivity().theme
                        )
                    )
                }
            }

            // Favorite icon
            val favoriteDrawable = when (favorite) {
                true -> R.drawable.ic_heart_filled
                false -> R.drawable.ic_heart
            }
            binding.favoriteButton.setImageResource(favoriteDrawable)
            if (favorite) binding.favoriteButton.imageTintList = ColorStateList.valueOf(
                resources.getColor(
                    R.color.red,
                    requireActivity().theme
                )
            )

            binding.title.text = item.name
            binding.subtitle.text = item.seriesName
            item.seriesName.let {
                binding.subtitle.text = it
                binding.subtitle.isVisible = true
            }
            binding.genres.text = genresString
            binding.year.text = dateString
            binding.playtime.text = runTime
            binding.officialRating.text = item.officialRating
            binding.communityRating.text = item.communityRating.toString()
            binding.description.text = item.overview
            bindBaseItemImage(binding.poster, item)
        }
    }

    private fun bindUiStateLoading() {}

    private fun bindUiStateError(uiState: MediaInfoViewModel.UiState.Error) {}

    private fun bindPlayerItems(items: PlayerItems) {
        navigateToPlayerActivity(items.items.toTypedArray())
        binding.playButton.setImageDrawable(
            ContextCompat.getDrawable(
                requireActivity(),
                R.drawable.ic_play
            )
        )
        binding.progressCircular.visibility = View.INVISIBLE
    }

    private fun bindPlayerItemsError(error: PlayerItemError) {
        Timber.e(error.message)

        binding.errorLayout.errorPanel.isVisible = true
        binding.playButton.setImageDrawable(
            ContextCompat.getDrawable(
                requireActivity(),
                R.drawable.ic_play
            )
        )
        binding.progressCircular.visibility = View.INVISIBLE
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