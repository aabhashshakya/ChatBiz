package com.hackathon.chat_biz.VIEW.FRAGMENTS.AUTHENTICATION

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.hackathon.chat_biz.R
import com.hackathon.chat_biz.VIEWMODEL.LoginViewModel

import com.hackathon.chat_biz.databinding.FragmentLoginBinding
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private const val TAG = "Login Fragment"

@AndroidEntryPoint
class LoginFragment() : Fragment(R.layout.fragment_login) {


    val loginViewModel: LoginViewModel by viewModels()

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var auth: FirebaseAuth


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentLoginBinding.bind(view)


        (activity as AppCompatActivity).supportActionBar?.show()
        (activity as AppCompatActivity).supportActionBar?.setDisplayShowTitleEnabled(false)
        (activity as AppCompatActivity).supportActionBar?.setHomeButtonEnabled(true)


        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)


        binding.apply {

            //checking if the text entered by the use is suitable, else show an error
            loginPassword.addTextChangedListener {
                if (it.toString().length < 8) {
                    loginPassword.error = "The password must be at least 8 characters."

                }
            }
            loginEmail.addTextChangedListener {
                if (!it.toString().endsWith(".com", true)) {
                    loginEmail.error = "Invalid Email"

                }
            }
        }

        //when button click we must login
        binding.loginLoginButton.setOnClickListener {
            //checking if the errors are still showing or if the EditTexts are null. if so we don't do anything
            if (binding.loginEmail.error == null && binding.loginPassword.error == null
                && binding.loginEmail.text?.isNotEmpty() == true && binding.loginPassword.text?.isNotEmpty() == true
            ) {

                binding.loginProgressBar.isVisible = true
                //when no errors in the EditText, WE TRY TO LOG IN
                binding.loginLoginButton.isEnabled = false

                loginViewModel.loginUser(
                    binding.loginEmail.text.toString(),
                    binding.loginPassword.text.toString()
                )
                    .addOnCompleteListener {
                        //if login successful, we navigate to our base activity
                        if (it.isSuccessful) {
                            binding.loginProgressBar.isVisible = false
                            Toast.makeText(context, "Successfully logged in!", Toast.LENGTH_SHORT)
                                .show()
                            Log.d(TAG, "User logged in.")

                            //this is how we hide the keyboard
                            try {
                                val imm: InputMethodManager =
                                    requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

                                imm.hideSoftInputFromWindow(
                                    requireActivity().currentFocus?.windowToken,
                                    0
                                )
                            } catch (e: Exception) {
                                Log.d(TAG, "onViewCreated: Key was not open: ${e.message}")
                            }
                            val action = LoginFragmentDirections.actionLoginFragmentToBaseFragment()
                            val navOptions =
                                NavOptions.Builder().setPopUpTo(R.id.welcomeFragment, true).build()
                            findNavController().navigate(action, navOptions)


                            //if login not successful, we set the error textview visible
                        } else {
                            binding.loginProgressBar.isVisible = false
                            binding.loginErrorText.text = it.exception?.message
                            binding.loginErrorText.isVisible = true
                            Log.d(TAG, "User log in error: ")
                            Log.d(TAG, " ${it.exception?.printStackTrace()}")
                            binding.loginLoginButton.isEnabled = true
                        }

                    }


            } else {
                //if EditText have errors, set the error textview to visible
                binding.loginErrorText.text = getString(R.string.invalid_credentials)
                binding.loginErrorText.isVisible = true
            }

        }


    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.loginErrorText.isVisible = false
        _binding = null

    }

}

