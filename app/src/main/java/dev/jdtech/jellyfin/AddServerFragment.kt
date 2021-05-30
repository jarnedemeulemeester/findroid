package dev.jdtech.jellyfin

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import dev.jdtech.jellyfin.databinding.FragmentAddServerBinding

class AddServerFragment : Fragment() {
    private var _binding: FragmentAddServerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAddServerBinding.inflate(inflater, container, false)
        val view = binding.root

        binding.buttonConnect.setOnClickListener { v: View ->
            v.findNavController().navigate(R.id.action_addServerFragment_to_loginFragment)
        }

        return view
    }
}