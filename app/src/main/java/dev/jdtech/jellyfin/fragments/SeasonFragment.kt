package dev.jdtech.jellyfin.fragments

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dev.jdtech.jellyfin.adapters.EpisodeListAdapter
import dev.jdtech.jellyfin.databinding.FragmentSeasonBinding
import dev.jdtech.jellyfin.viewmodels.SeasonViewModel
import dev.jdtech.jellyfin.viewmodels.SeasonViewModelFactory

class SeasonFragment : Fragment() {

    private lateinit var viewModel: SeasonViewModel
    private lateinit var binding: FragmentSeasonBinding

    private val args: SeasonFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSeasonBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val viewModelFactory = SeasonViewModelFactory(
            requireNotNull(this.activity).application,
            args.seriesId,
            args.seasonId
        )
        viewModel = ViewModelProvider(this, viewModelFactory).get(SeasonViewModel::class.java)
        binding.viewModel = viewModel
        binding.episodesRecyclerView.adapter =
            EpisodeListAdapter(EpisodeListAdapter.OnClickListener {
                findNavController().navigate(
                    SeasonFragmentDirections.actionSeasonFragmentToEpisodeBottomSheetFragment(
                        it.id
                    )
                )
            })
        binding.seriesName.text = args.seriesName
        binding.seasonName.text = args.seasonName
        binding.seriesId = args.seriesId
        binding.seasonId = args.seasonId
    }

}