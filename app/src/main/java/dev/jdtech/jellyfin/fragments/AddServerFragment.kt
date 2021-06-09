package dev.jdtech.jellyfin.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.databinding.FragmentAddServerBinding
import dev.jdtech.jellyfin.viewmodels.AddServerViewModel
import dev.jdtech.jellyfin.viewmodels.AddServerViewModelFactory

class AddServerFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val application = requireNotNull(this.activity).application
        val binding = FragmentAddServerBinding.inflate(inflater)
        val viewModelFactory = AddServerViewModelFactory(application)
        val viewModel = ViewModelProvider(this, viewModelFactory).get(AddServerViewModel::class.java)

        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        binding.buttonConnect.setOnClickListener { v: View ->
            viewModel.checkServer(binding.editTextServerAddress.text.toString())
            //v.findNavController().navigate(R.id.action_addServerFragment_to_loginFragment)
        }

        return binding.root
    }
}