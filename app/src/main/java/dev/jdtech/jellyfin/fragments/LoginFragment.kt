package dev.jdtech.jellyfin.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.databinding.FragmentLoginBinding
import dev.jdtech.jellyfin.viewmodels.LoginViewModel
import dev.jdtech.jellyfin.viewmodels.LoginViewModelFactory

class LoginFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val application = requireNotNull(this.activity).application
        val binding = FragmentLoginBinding.inflate(inflater)
        val viewModelFactory = LoginViewModelFactory(application)
        val viewModel = ViewModelProvider(this, viewModelFactory).get(LoginViewModel::class.java)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        binding.buttonLogin.setOnClickListener {
            val username = binding.editTextUsername.text.toString()
            val password = binding.editTextPassword.text.toString()

            binding.progressCircular.visibility = View.VISIBLE
            viewModel.login(username, password)
        }

        viewModel.error.observe(viewLifecycleOwner, {
            binding.progressCircular.visibility = View.GONE
            binding.editTextUsernameLayout.error = it
        })

        viewModel.navigateToMain.observe(viewLifecycleOwner, {
            if (it) {
                findNavController().navigate(R.id.action_loginFragment_to_mainActivity)
                viewModel.doneNavigatingToMain()
            }
        })

        return binding.root
    }
}