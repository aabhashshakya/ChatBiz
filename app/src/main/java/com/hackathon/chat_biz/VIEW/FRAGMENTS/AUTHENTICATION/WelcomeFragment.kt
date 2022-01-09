package com.hackathon.chat_biz.VIEW.FRAGMENTS.AUTHENTICATION

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.hackathon.chat_biz.R
import com.hackathon.chat_biz.databinding.FragmentWelcomeBinding
import dagger.hilt.android.AndroidEntryPoint

private const val TAG = "Welcome Fragment"

@AndroidEntryPoint
class WelcomeFragment : Fragment(R.layout.fragment_welcome) {

    private var _binding: FragmentWelcomeBinding? = null
    private val binding get() = _binding!!


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentWelcomeBinding.bind(view)

        (activity as AppCompatActivity).supportActionBar?.hide()


        //login button click
        binding.welcomeLogin.setOnClickListener {
            Log.d(TAG, "Login clicked: Navigating to login fragment")
            val action = WelcomeFragmentDirections.actionWelcomeFragmentToLoginFragment()
            findNavController().navigate(action)
        }

        //register button click
        binding.welcomeRegister.setOnClickListener {
            Log.d(TAG, "Register clicked: Navigating to register fragment")
            val action = WelcomeFragmentDirections.actionWelcomeFragmentToRegisterFragment()
            findNavController().navigate(action)
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}