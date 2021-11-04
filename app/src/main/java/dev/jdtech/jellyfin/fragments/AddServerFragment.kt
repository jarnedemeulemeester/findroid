package dev.jdtech.jellyfin.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.databinding.FragmentAddServerBinding
import dev.jdtech.jellyfin.viewmodels.AddServerViewModel

@AndroidEntryPoint
class AddServerFragment : Fragment() {

    private lateinit var binding: FragmentAddServerBinding
    private val viewModel: AddServerViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddServerBinding.inflate(inflater)

        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel

        binding.buttonConnect.setOnClickListener {
            val serverAddress = binding.editTextServerAddress.text.toString()
            if (serverAddress.isNotBlank()) {
                viewModel.checkServer(serverAddress, resources)
                binding.progressCircular.visibility = View.VISIBLE
                binding.editTextServerAddressLayout.error = ""
            } else {
                binding.editTextServerAddressLayout.error = resources.getString(R.string.add_server_error_empty_address)
            }
        }

        viewModel.navigateToLogin.observe(viewLifecycleOwner, {
            if (it) {
                navigateToLoginFragment()
            }
            binding.progressCircular.visibility = View.GONE
        })

        viewModel.error.observe(viewLifecycleOwner, {
            binding.editTextServerAddressLayout.error = it
        })

        return binding.root
    }

    private fun navigateToLoginFragment() {
        findNavController().navigate(AddServerFragmentDirections.actionAddServerFragment3ToLoginFragment2())
        viewModel.onNavigateToLoginDone()
    }
}