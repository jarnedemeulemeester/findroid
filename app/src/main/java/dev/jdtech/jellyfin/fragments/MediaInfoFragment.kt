package dev.jdtech.jellyfin.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dev.jdtech.jellyfin.adapters.PersonListAdapter
import dev.jdtech.jellyfin.adapters.ViewItemListAdapter
import dev.jdtech.jellyfin.databinding.FragmentMediaInfoBinding
import dev.jdtech.jellyfin.viewmodels.MediaInfoViewModel
import dev.jdtech.jellyfin.viewmodels.MediaInfoViewModelFactory

class MediaInfoFragment : Fragment() {

    private lateinit var binding: FragmentMediaInfoBinding
    private lateinit var viewModel: MediaInfoViewModel

    private val args: MediaInfoFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMediaInfoBinding.inflate(inflater, container, false)

        binding.lifecycleOwner = this

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModelFactory =
            MediaInfoViewModelFactory(requireNotNull(this.activity).application, args.itemId)
        viewModel = ViewModelProvider(this, viewModelFactory).get(MediaInfoViewModel::class.java)
        binding.viewModel = viewModel

        viewModel.item.observe(viewLifecycleOwner, { item ->
            if (item.originalTitle != item.name) {
                binding.originalTitle.visibility = View.VISIBLE
            } else {
                binding.originalTitle.visibility = View.GONE
            }
            if (item.trailerCount != null && item.trailerCount!! < 1) {
                binding.trailerButton.visibility = View.GONE
            }
        })

        binding.trailerButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(viewModel.item.value?.remoteTrailers?.get(0)?.url))
            startActivity(intent)
        }

        binding.nextUp.setOnClickListener {
            findNavController().navigate(MediaInfoFragmentDirections.actionMediaInfoFragmentToEpisodeBottomSheetFragment(viewModel.nextUp.value!!.id))
        }

        binding.seasonsRecyclerView.adapter =
            ViewItemListAdapter(ViewItemListAdapter.OnClickListener {
                findNavController().navigate(
                    MediaInfoFragmentDirections.actionMediaInfoFragmentToSeasonFragment(
                        it.seriesId!!,
                        it.id,
                        it.seriesName,
                        it.name
                    )
                )
            }, fixedWidth = true)
        binding.peopleRecyclerView.adapter = PersonListAdapter()
    }

}