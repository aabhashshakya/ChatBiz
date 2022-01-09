package com.hackathon.chat_biz.VIEW.FRAGMENTS.AUTHENTICATION

import android.content.Context.INPUT_METHOD_SERVICE
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.hackathon.chat_biz.R
import com.hackathon.chat_biz.VIEWMODEL.RegisterViewModel
import com.hackathon.chat_biz.databinding.FragmentRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


private const val TAG = "Register Fragment"

@AndroidEntryPoint
class RegisterFragment : Fragment(R.layout.fragment_register) {


    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!





    @Inject
    lateinit var auth: FirebaseAuth

    val registerViewModel: RegisterViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentRegisterBinding.bind(view)

        (activity as AppCompatActivity).supportActionBar?.show()
        (activity as AppCompatActivity).supportActionBar?.setDisplayShowTitleEnabled(false)
        (activity as AppCompatActivity).supportActionBar?.setHomeButtonEnabled(true)
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)



        binding.apply {

            //checking if the INFO entered by the use is suitable, else show an error
            registerPassword.addTextChangedListener {
                if (it.toString().length < 8) {
                    registerPassword.error = "The password must be at least 8 characters."

                }
            }
            registerEmail.addTextChangedListener {
                if (!it.toString().endsWith(".com", true)) {
                    registerEmail.error = "Invalid Email format"

                }
            }
            registerUsername.addTextChangedListener {
                if (it.toString().length < 2 || it.toString().length > 25) {
                    registerUsername.error = "Invalid Username"

                }
            }

        }


        //when button click we must register
        binding.registerSignup.setOnClickListener {


            //checking if the errors are still showing or if the EditTexts are NULL. if so we don't do anything
            if (binding.registerEmail.error == null && binding.registerPassword.error == null && binding.registerUsername.error == null
                && binding.registerEmail.text?.isNotEmpty() == true &&
                binding.registerPassword.text?.isNotEmpty() == true && binding.registerUsername.text?.isNotEmpty() == true
            ) {
                binding.registerProgressBar.isVisible = true
                //when no errors in the EditText, WE TRY TO REGISTER THE USER
                binding.registerSignup.isEnabled = false
                val username = binding.registerUsername.text.toString()
                val email = binding.registerEmail.text.toString()
                val password = binding.registerPassword.text.toString()


                //registering the user to firebase auth
                registerViewModel.registerUser(email, password)
                    .addOnCompleteListener {

                        if (it.isSuccessful) {
                            binding.registerProgressBar.isVisible = false
                            binding.registerSignup.isEnabled = false

                            //setting up the display name for the user that was authenticated
                            it.result?.user?.updateProfile(
                                UserProfileChangeRequest.Builder()
                                    .setDisplayName(username)
                                    .build()
                            )

                            //add user to database
                            registerViewModel.addUserToDatabase(username, email)

                            //this is how we hide the keyboard
                            try {
                                val imm: InputMethodManager =
                                    requireContext().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager

                                imm.hideSoftInputFromWindow(
                                    requireActivity().currentFocus?.windowToken,
                                    0
                                )
                            } catch (e: Exception) {
                                Log.d(TAG, "onViewCreated: Key was not open: ${e.message}")
                            }

                            Log.d(TAG, "User registered: ${it.result?.user?.displayName}")
                            val action =
                                RegisterFragmentDirections.actionRegisterFragmentToBaseFragment()
                            val navOptions = NavOptions.Builder().setPopUpTo(
                                R.id.welcomeFragment,
                                true
                            ).build()
                            findNavController().navigate(action, navOptions)


                            //IF REGISTRATION NOT SUCCESSFUL, WE DISPLAY THE ERROR IN THE TEXTVIEW
                        } else {
                            binding.registerProgressBar.isVisible = false
                            binding.registerErrorText.text = it.exception?.message
                            binding.registerErrorText.isVisible = true
                            Log.d(TAG, "User register error: ")
                            Log.d(TAG, " ${it.exception?.printStackTrace()}")


                        }

                    }


            } else {
                //if EditText have errors, set the error textview to visible
                binding.registerErrorText.text = getString(R.string.invalid_credentials)
                binding.registerErrorText.isVisible = true
                binding.registerSignup.isEnabled = true
            }

        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.registerErrorText.isVisible = false
        _binding = null
    }
}