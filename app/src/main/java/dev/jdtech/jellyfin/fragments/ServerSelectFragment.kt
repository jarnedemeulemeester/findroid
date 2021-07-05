package dev.jdtech.jellyfin.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.database.ServerDatabase
import dev.jdtech.jellyfin.databinding.FragmentServerSelectBinding
import dev.jdtech.jellyfin.dialogs.DeleteServerDialogFragment
import dev.jdtech.jellyfin.adapters.ServerGridAdapter
import dev.jdtech.jellyfin.viewmodels.ServerSelectViewModelFactory
import dev.jdtech.jellyfin.viewmodels.ServerSelectViewModel


class ServerSelectFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentServerSelectBinding.inflate(inflater)

        val application = requireNotNull(this.activity).application

        val dataSource = ServerDatabase.getInstance(application).serverDatabaseDao

        val viewModelFactory = ServerSelectViewModelFactory(dataSource, application)
        val viewModel: ServerSelectViewModel by viewModels { viewModelFactory }

        binding.lifecycleOwner = this
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
            this.findNavController().navigate(R.id.action_serverSelectFragment_to_addServerFragment)
        }

        viewModel.navigateToMain.observe(viewLifecycleOwner, {
            if (it) {
                findNavController().navigate(R.id.action_serverSelectFragment_to_mainActivity)
                viewModel.doneNavigatingToMain()
            }
        })

        return binding.root
    }
}