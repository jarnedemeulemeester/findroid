package dev.jdtech.jellyfin.serverselect

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.database.ServerDatabase
import dev.jdtech.jellyfin.databinding.FragmentServerSelectBinding
import dev.jdtech.jellyfin.dialogs.DeleteServerDialogFragment


class ServerSelectFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentServerSelectBinding.inflate(inflater)

        val application = requireNotNull(this.activity).application

        val dataSource = ServerDatabase.getInstance(application).serverDatabaseDao

        val viewModelFactory = ServerSelectViewModelFactory(dataSource, application)
        val viewModel = ViewModelProvider(this, viewModelFactory).get(ServerSelectViewModel::class.java)

        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.serversRecyclerView.adapter = ServerGridAdapter(ServerGridAdapter.OnClickListener { server ->
            Toast.makeText(application, "You selected server ${server.name}", Toast.LENGTH_SHORT).show()
        }, ServerGridAdapter.OnLongClickListener { server ->
            DeleteServerDialogFragment(viewModel, server).show(parentFragmentManager, "deleteServer")
            true
        })

        binding.buttonAddServer.setOnClickListener {
            this.findNavController().navigate(R.id.action_serverSelectFragment_to_addServerFragment)
        }
        return binding.root
    }
}