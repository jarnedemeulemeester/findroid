package dev.jdtech.jellyfin.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
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

        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        binding.buttonConnect.setOnClickListener {
            val serverAddress = binding.editTextServerAddress.text.toString()
            if (serverAddress.isNotBlank()) {
                viewModel.checkServer(serverAddress)
                binding.progressCircular.visibility = View.VISIBLE
            } else {
                binding.editTextServerAddressLayout.error = "Empty server address"
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
        findNavController().navigate(AddServerFragmentDirections.actionAddServerFragmentToLoginFragment())
        viewModel.onNavigateToLoginDone()
    }
}