package com.hackathon.chat_biz.VIEWMODEL

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.ViewModel
import com.hackathon.chat_biz.REPOSITORY.RegisterRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult

private const val TAG = "ProfileViewModel"

class RegisterViewModel @ViewModelInject constructor(
    private val repository: RegisterRepository
) : ViewModel() {

    fun registerUser(email: String, password: String): Task<AuthResult> {
        return repository.registerUser(email, password)

    }

    fun addUserToDatabase(username: String, email: String) {
        repository.addUserToDatabase(username, email)
    }


}