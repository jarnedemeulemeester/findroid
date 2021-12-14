package dev.jdtech.jellyfin.tv.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
    private val detailViewModel: MediaDetailViewModel by viewModels()
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
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.viewModel = viewModel

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.onUiState(viewLifecycleOwner.lifecycleScope) { uiState ->
                    Timber.d("$uiState")
                    when (uiState) {
                        is MediaInfoViewModel.UiState.Normal -> Unit
                        is MediaInfoViewModel.UiState.Loading -> Unit
                        is MediaInfoViewModel.UiState.Error -> Unit
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
    }

    /*private fun bindState(state: MediaDetailViewModel.State) {
        playerViewModel.onPlaybackRequested(lifecycleScope) { state ->
            when (state) {
                is PlayerItemError -> bindPlayerItemsError(state)
                is PlayerItems -> bindPlayerItems(state)
            }
        }

        when (state.media) {
            is Movie -> binding.title.text = state.media.title
            is TvShow -> with(binding.subtitle) {
                binding.title.text = state.media.episode
                text = state.media.show
                isVisible = true
            }
        }
    }*/

    private fun bindUiStateNormal(uiState: MediaInfoViewModel.UiState.Normal) {
        uiState.apply {
            binding.seasonTitle.isVisible = seasons.isNotEmpty()
            val seasonsAdapter = binding.seasonsRow.gridView.adapter as ViewItemListAdapter
            seasonsAdapter.submitList(seasons)
            binding.castTitle.isVisible = actors.isNotEmpty()
            val actorsAdapter = binding.castRow.gridView.adapter as PersonListAdapter
            actorsAdapter.submitList(actors)
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

    private fun bindActions(state: MediaDetailViewModel.State) {
        binding.playButton.setOnClickListener {
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
                setImageResource(R.drawable.ic_check_filled)
                setOnClickListener { viewModel.markAsUnplayed(args.itemId) }
            }
        } else {
            with(binding.checkButton) {
                setImageResource(R.drawable.ic_check)
                setOnClickListener { viewModel.markAsPlayed(args.itemId) }
            }
        }

        if (state.isFavorite) {
            with(binding.favoriteButton) {
                setImageResource(R.drawable.ic_heart_filled)
                setOnClickListener { viewModel.unmarkAsFavorite(args.itemId) }
            }
        } else {
            with(binding.favoriteButton) {
                setImageResource(R.drawable.ic_heart)
                setOnClickListener { viewModel.markAsFavorite(args.itemId) }
            }
        }

        binding.backButton.setOnClickListener { activity?.onBackPressed() }
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