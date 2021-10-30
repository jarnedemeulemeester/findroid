package dev.jdtech.jellyfin.tv.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.databinding.TvLoginFragmentBinding
import dev.jdtech.jellyfin.viewmodels.LoginViewModel

@AndroidEntryPoint
class TvLoginFragment : Fragment() {

    private val viewModel: LoginViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = TvLoginFragmentBinding.inflate(inflater)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel

        binding.buttonLogin.setOnClickListener {
            val username = binding.username.text.toString()
            val password = binding.password.text.toString()

            binding.progressCircular.visibility = View.VISIBLE
            viewModel.login(username, password)
        }

        viewModel.error.observe(viewLifecycleOwner, {
            binding.progressCircular.visibility = View.GONE
            binding.username.error = it
        })

        viewModel.navigateToMain.observe(viewLifecycleOwner, {
            if (it) {
                navigateToMainActivity()
            }
        })

        return binding.root
    }

    private fun navigateToMainActivity() {
        findNavController().navigate(TvLoginFragmentDirections.actionLoginFragmentToNavigationHome())
        viewModel.doneNavigatingToMain()
    }
}