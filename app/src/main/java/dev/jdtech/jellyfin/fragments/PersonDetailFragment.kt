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
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.adapters.ViewItemListAdapter
import dev.jdtech.jellyfin.bindItemImage
import dev.jdtech.jellyfin.databinding.FragmentPersonDetailBinding
import dev.jdtech.jellyfin.dialogs.ErrorDialogFragment
import dev.jdtech.jellyfin.utils.checkIfLoginRequired
import dev.jdtech.jellyfin.viewmodels.PersonDetailViewModel
import org.jellyfin.sdk.model.api.BaseItemDto

@AndroidEntryPoint
internal class PersonDetailFragment : Fragment() {

    private lateinit var binding: FragmentPersonDetailBinding
    private val viewModel: PersonDetailViewModel by viewModels()

    private val args: PersonDetailFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPersonDetailBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.moviesList.adapter = adapter()
        binding.showList.adapter = adapter()

        viewModel.data.observe(viewLifecycleOwner) { data ->
            binding.name.text = data.name
            binding.overview.text = data.overview

            setupOverviewExpansion()

            bindItemImage(binding.personImage, data.dto)
        }

        viewModel.finishedLoading.observe(viewLifecycleOwner, {
            binding.loadingIndicator.visibility = if (it) View.GONE else View.VISIBLE
        })

        viewModel.error.observe(viewLifecycleOwner, { error ->
            if (error != null) {
                checkIfLoginRequired(error)
                binding.errorLayout.errorPanel.visibility = View.VISIBLE
                binding.fragmentContent.visibility = View.GONE
            } else {
                binding.errorLayout.errorPanel.visibility = View.GONE
                binding.fragmentContent.visibility = View.VISIBLE
            }
        })

        binding.errorLayout.errorRetryButton.setOnClickListener {
            viewModel.loadData(args.personId)
        }

        binding.errorLayout.errorDetailsButton.setOnClickListener {
            ErrorDialogFragment(viewModel.error.value ?: getString(R.string.unknown_error)).show(
                parentFragmentManager,
                "errordialog"
            )
        }

        viewModel.loadData(args.personId)
    }

    private fun adapter() = ViewItemListAdapter(
        fixedWidth = true,
        onClickListener = ViewItemListAdapter.OnClickListener { navigateToMediaInfoFragment(it) }
    )

    private fun setupOverviewExpansion() = binding.overview.post {
        binding.readAll.setOnClickListener {
            with(binding.overview) {
                if (layoutParams.height == ConstraintLayout.LayoutParams.WRAP_CONTENT) {
                    updateLayoutParams { height = ConstraintLayout.LayoutParams.MATCH_CONSTRAINT }
                    binding.readAll.text = getString(R.string.view_all)
                    binding.overviewGradient.isVisible = true
                } else {
                    updateLayoutParams { height = ConstraintLayout.LayoutParams.WRAP_CONTENT }
                    binding.readAll.text = getString(R.string.hide)
                    binding.overviewGradient.isVisible = false
                }
            }

        }
    }

    private fun navigateToMediaInfoFragment(item: BaseItemDto) {
        findNavController().navigate(
            PersonDetailFragmentDirections.actionPersonDetailFragmentToMediaInfoFragment(
                itemId = item.id,
                itemName = item.name,
                itemType = item.type ?: "Unknown"
            )
        )
    }
}