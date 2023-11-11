package dev.jdtech.jellyfin.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.adapters.ViewItemListAdapter
import dev.jdtech.jellyfin.bindItemImage
import dev.jdtech.jellyfin.databinding.FragmentPersonDetailBinding
import dev.jdtech.jellyfin.dialogs.ErrorDialogFragment
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.FindroidMovie
import dev.jdtech.jellyfin.models.FindroidShow
import dev.jdtech.jellyfin.utils.checkIfLoginRequired
import dev.jdtech.jellyfin.viewmodels.PersonDetailViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import dev.jdtech.jellyfin.core.R as CoreR

@AndroidEntryPoint
internal class PersonDetailFragment : Fragment() {

    private lateinit var binding: FragmentPersonDetailBinding
    private val viewModel: PersonDetailViewModel by viewModels()

    private val args: PersonDetailFragmentArgs by navArgs()

    private lateinit var errorDialog: ErrorDialogFragment

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentPersonDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.moviesList.adapter = adapter()
        binding.showList.adapter = adapter()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    Timber.d("$uiState")
                    when (uiState) {
                        is PersonDetailViewModel.UiState.Normal -> bindUiStateNormal(uiState)
                        is PersonDetailViewModel.UiState.Loading -> bindUiStateLoading()
                        is PersonDetailViewModel.UiState.Error -> bindUiStateError(uiState)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.loadData(args.personId)
            }
        }

        binding.errorLayout.errorRetryButton.setOnClickListener {
            viewModel.loadData(args.personId)
        }

        binding.errorLayout.errorDetailsButton.setOnClickListener {
            errorDialog.show(parentFragmentManager, ErrorDialogFragment.TAG)
        }
    }

    private fun bindUiStateNormal(uiState: PersonDetailViewModel.UiState.Normal) {
        uiState.apply {
            binding.name.text = data.name
            binding.overview.text = data.overview
            setupOverviewExpansion()
            bindItemImage(binding.personImage, data.dto)

            if (starredIn.movies.isNotEmpty()) {
                binding.movieLabel.isVisible = true
                val moviesAdapter = binding.moviesList.adapter as ViewItemListAdapter
                moviesAdapter.submitList(starredIn.movies)
            }
            if (starredIn.shows.isNotEmpty()) {
                binding.showLabel.isVisible = true
                val showsAdapter = binding.showList.adapter as ViewItemListAdapter
                showsAdapter.submitList(starredIn.shows)
            }
        }

        binding.loadingIndicator.isVisible = false
        binding.fragmentContent.isVisible = true
        binding.errorLayout.errorPanel.isVisible = false
    }

    private fun bindUiStateLoading() {
        binding.loadingIndicator.isVisible = true
        binding.errorLayout.errorPanel.isVisible = false
    }

    private fun bindUiStateError(uiState: PersonDetailViewModel.UiState.Error) {
        errorDialog = ErrorDialogFragment.newInstance(uiState.error)
        binding.loadingIndicator.isVisible = false
        binding.fragmentContent.isVisible = false
        binding.errorLayout.errorPanel.isVisible = true
        checkIfLoginRequired(uiState.error.message)
    }

    private fun adapter() = ViewItemListAdapter(
        fixedWidth = true,
        onClickListener = { navigateToMediaItem(it) },
    )

    private fun setupOverviewExpansion() = binding.overview.post {
        binding.readAll.setOnClickListener {
            with(binding.overview) {
                if (layoutParams.height == ConstraintLayout.LayoutParams.WRAP_CONTENT) {
                    updateLayoutParams { height = resources.getDimension(CoreR.dimen.person_detail_overview_height).toInt() }
                    binding.readAll.text = getString(CoreR.string.view_all)
                    binding.overviewGradient.isVisible = true
                } else {
                    updateLayoutParams { height = ConstraintLayout.LayoutParams.WRAP_CONTENT }
                    binding.readAll.text = getString(CoreR.string.hide)
                    binding.overviewGradient.isVisible = false
                }
            }
        }
    }

    private fun navigateToMediaItem(item: FindroidItem) {
        when (item) {
            is FindroidMovie -> {
                findNavController().navigate(
                    PersonDetailFragmentDirections.actionPersonDetailFragmentToMovieFragment(
                        itemId = item.id,
                        itemName = item.name,
                    ),
                )
            }
            is FindroidShow -> {
                findNavController().navigate(
                    PersonDetailFragmentDirections.actionPersonDetailFragmentToShowFragment(
                        itemId = item.id,
                        itemName = item.name,
                    ),
                )
            }
        }
    }
}
