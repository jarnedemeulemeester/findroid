package dev.jdtech.jellyfin.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.adapters.ViewItemListAdapter
import dev.jdtech.jellyfin.bindItemImage
import dev.jdtech.jellyfin.databinding.FragmentPersonDetailBinding
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
            binding.loadingIndicator.isVisible = false
        }

        viewModel.loadData(args.personId)
    }

    private fun adapter() = ViewItemListAdapter(
        fixedWidth = true,
        onClickListener = ViewItemListAdapter.OnClickListener { navigateToMediaInfoFragment(it) }
    )

    private fun setupOverviewExpansion() = binding.overview.post {
        binding.readAll.isVisible = binding.overview.isCollapsed()

        binding.readAll.setOnClickListener {
            with(binding.overview) {
                if (maxLines > 5) {
                    maxLines = 5
                    binding.readAll.text = getString(R.string.view_all)
                } else {
                    maxLines = Int.MAX_VALUE
                    binding.readAll.text = getString(R.string.hide)
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

    private fun TextView.isCollapsed(): Boolean = layout.getEllipsisCount(layout.lineCount - 1) > 0
}