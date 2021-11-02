package dev.jdtech.jellyfin.tv.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.databinding.TvAddServerFragmentBinding
import dev.jdtech.jellyfin.viewmodels.AddServerViewModel

@AndroidEntryPoint
internal class TvAddServerFragment: Fragment() {

    private lateinit var binding: TvAddServerFragmentBinding
    private val viewModel: AddServerViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = TvAddServerFragmentBinding.inflate(inflater)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel

        binding.buttonConnect.setOnClickListener {
            val serverAddress = binding.serverAddress.text.toString()
            if (serverAddress.isNotBlank()) {
                viewModel.checkServer(serverAddress, resources)
                binding.progressCircular.visibility = View.VISIBLE
            } else {
                binding.serverAddress.error = resources.getString(R.string.add_server_empty_error)
            }
        }

        viewModel.navigateToLogin.observe(viewLifecycleOwner, {
            if (it) {
                navigateToLoginFragment()
            }
            binding.progressCircular.visibility = View.GONE
        })

        viewModel.error.observe(viewLifecycleOwner, {
            binding.serverAddress.error = it
        })

        return binding.root
    }

    private fun navigateToLoginFragment() {
        findNavController().navigate(TvAddServerFragmentDirections.actionAddServerFragmentToLoginFragment())
        viewModel.onNavigateToLoginDone()
    }
}