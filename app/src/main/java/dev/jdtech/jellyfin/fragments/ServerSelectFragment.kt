package dev.jdtech.jellyfin.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.databinding.FragmentServerSelectBinding
import dev.jdtech.jellyfin.dialogs.DeleteServerDialogFragment
import dev.jdtech.jellyfin.adapters.ServerGridAdapter
import dev.jdtech.jellyfin.viewmodels.ServerSelectViewModel

@AndroidEntryPoint
class ServerSelectFragment : Fragment() {

    private lateinit var binding: FragmentServerSelectBinding
    private val viewModel: ServerSelectViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentServerSelectBinding.inflate(inflater)

        binding.lifecycleOwner = viewLifecycleOwner

        binding.viewModel = viewModel

        binding.serversRecyclerView.adapter =
            ServerGridAdapter(ServerGridAdapter.OnClickListener { server ->
                viewModel.connectToServer(server)
            }, ServerGridAdapter.OnLongClickListener { server ->
                DeleteServerDialogFragment(viewModel, server).show(
                    parentFragmentManager,
                    "deleteServer"
                )
                true
            })

        binding.buttonAddServer.setOnClickListener {
            navigateToAddServerFragment()
        }

        viewModel.navigateToMain.observe(viewLifecycleOwner, {
            if (it) {
                navigateToMainActivity()
            }
        })

        return binding.root
    }

    private fun navigateToAddServerFragment() {
        findNavController().navigate(
            ServerSelectFragmentDirections.actionServerSelectFragment2ToAddServerFragment3()
        )
    }

    private fun navigateToMainActivity() {
        findNavController().navigate(ServerSelectFragmentDirections.actionServerSelectFragmentToHomeFragment())
        viewModel.doneNavigatingToMain()
    }
}