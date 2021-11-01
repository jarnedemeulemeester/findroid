package dev.jdtech.jellyfin.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.databinding.FragmentLoginBinding
import dev.jdtech.jellyfin.viewmodels.LoginViewModel

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private lateinit var binding: FragmentLoginBinding
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoginBinding.inflate(inflater)

        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel

        binding.editTextPassword.setOnEditorActionListener { _, actionId, _ ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_GO -> {
                    login()
                    true
                }
                else -> false
            }
        }

        binding.buttonLogin.setOnClickListener {
            login()
        }

        viewModel.navigateToMain.observe(viewLifecycleOwner, {
            if (it) {
                navigateToMainActivity()
            }
            binding.progressCircular.visibility = View.GONE
        })

        viewModel.error.observe(viewLifecycleOwner, {
            binding.editTextUsernameLayout.error = it
            if (it.isNullOrBlank()) {
                return@observe
            }
            binding.progressCircular.visibility = View.GONE
        })

        return binding.root
    }

    private fun login() {
        val username = binding.editTextUsername.text.toString()
        val password = binding.editTextPassword.text.toString()
        binding.progressCircular.visibility = View.VISIBLE
        viewModel.login(username, password)
    }

    private fun navigateToMainActivity() {
        findNavController().navigate(LoginFragmentDirections.actionLoginFragment2ToNavigationHome())
        viewModel.doneNavigatingToMain()
    }
}